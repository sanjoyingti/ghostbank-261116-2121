package com.aibuilt.ghostbank.data.net

import android.util.Log
import com.aibuilt.ghostbank.data.db.HostLogDao
import com.aibuilt.ghostbank.data.db.HostLogEntity
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.data.settings.ProxyEntry
import com.aibuilt.ghostbank.data.settings.SettingsStore
import com.aibuilt.ghostbank.model.HopResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Single HTTP stack (OkHttp). Adds:
 *  - proxy pool rotation and Tor mode
 *  - kill-switch: no traffic when no exit is configured and one is required
 *  - DNS-over-HTTPS
 *  - Chrome header order, sec-ch-ua client hints, keep-alives and padding
 *  - every exchange is written to the raw host log
 */
class GhostClient(
    private val settings: SettingsStore,
    private val redirectFollower: RedirectFollower,
    private val hostLogDao: HostLogDao
) {

    private val rotateIndex = AtomicInteger(0)
    private val velocity = VelocityWindow()
    private val mutex = Mutex()

    /** DNS-over-HTTPS + header order interceptor, applied to every base client. */
    private fun baseClient(proxy: Proxy?): OkHttpClient {
        val b = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(false)
            .followSslRedirects(false)

        if (proxy != null) b.proxy(proxy)

        b.dns(DohDns())
        b.addInterceptor(HeaderOrderInterceptor())
        return b.build()
    }

    private suspend fun currentSettings(): GhostSettings = settings.snapshot()

    private suspend fun proxyFor(cfg: GhostSettings): Proxy? {
        if (cfg.torMode) {
            // Orbot exposes a SOCKS5 listener here.
            return Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050))
        }
        val pool = cfg.proxyPool
        if (pool.isEmpty()) return null
        val entry: ProxyEntry = pool[Math.floorMod(rotateIndex.getAndIncrement(), pool.size)]
        val parts = entry.hostPort.split(":")
        val host = parts.firstOrNull() ?: return null
        val port = parts.getOrNull(1)?.toIntOrNull() ?: if (entry.isSocks) 1080 else 8080
        return if (entry.isSocks) {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port))
        } else {
            Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
        }
    }

    /** Thrown when the kill-switch blocks a send. */
    class KillSwitchException(message: String) : Exception(message)

    /**
     * Raw call honouring the kill-switch. Returns null when the transport is dead and the
     * caller has to decide what that means (never a fake success).
     */
    suspend fun callOrNull(
        runId: String,
        stage: String,
        url: String,
        headers: Map<String, String>,
        method: String = "GET",
        body: String? = null
    ): HopResult? = withContext(Dispatchers.IO) {
        val cfg = currentSettings()
        if (cfg.killSwitch && !cfg.exitConfigured && cfg.proxyRequired) {
            return@withContext null
        }
        mutex.withLock { velocity.acquire(cfg) }

        val proxy = proxyFor(cfg)
        val client = baseClient(proxy)

        var builder = Request.Builder().url(url)
        if (cfg.padding) builder = builder.addHeader("X-Pad", Padding.value())
        headers.forEach { (k, v) ->
            if (!k.equals("X-Pad", true)) builder = builder.addHeader(k, v)
        }
        val request = if (method == "POST" && body != null) {
            builder.post(okhttp3.RequestBody.create(null, body)).build()
        } else {
            builder.get().build()
        }

        val started = System.currentTimeMillis()
        val walk = try {
            redirectFollower.follow(client, request)
        } catch (e: Exception) {
            Log.w(TAG, "call failed $url: ${e.message}")
            safeLog(runId, stage, method, url, url, -1, 0, System.currentTimeMillis() - started, headers, e.toString(), cfg)
            return@withContext null
        }
        val elapsed = System.currentTimeMillis() - started
        safeLog(
            runId, stage, method, url, walk.finalUrl, walk.status, walk.hops, elapsed,
            headers, walk.body.take(20_000), cfg
        )
        client.dispatcher.executorService.shutdown()
        HopResult(walk.status, walk.body, walk.hops, walk.finalUrl, walk.contentType)
    }

    /** Ping a homepage; 2xx and 3xx are alive. */
    suspend fun ping(runId: String, stage: String, url: String, headers: Map<String, String>): HopResult? =
        callOrNull(runId, stage, url, headers, "GET", null)

    private suspend fun safeLog(
        runId: String,
        stage: String,
        method: String,
        url: String,
        finalUrl: String,
        status: Int,
        hops: Int,
        elapsed: Long,
        headers: Map<String, String>,
        responseBody: String,
        cfg: GhostSettings
    ) {
        runCatching {
            hostLogDao.insert(
                HostLogEntity(
                    runId = runId,
                    stage = stage,
                    method = method,
                    url = url,
                    finalUrl = finalUrl,
                    status = status,
                    hops = hops,
                    elapsedMs = elapsed,
                    requestHeaders = headers.entries.joinToString("\n") { "${it.key}: ${it.value}" } +
                        "\nexit=${if (cfg.torMode) "tor" else cfg.proxyPool.size.toString() + " proxies"}",
                    responseBody = responseBody
                )
            )
        }
    }

    /** Simple velocity gate so we never exceed the configured requests/minute. */
    private class VelocityWindow {
        private val stamps = ArrayDeque<Long>()

        suspend fun acquire(cfg: GhostSettings) {
            val perMinute = cfg.maxRequestsPerMinute.coerceAtLeast(1)
            while (true) {
                val now = System.currentTimeMillis()
                while (stamps.isNotEmpty() && now - stamps.first() > 60_000) stamps.removeFirst()
                if (stamps.size < perMinute) {
                    stamps.addLast(now)
                    return
                }
                val wait = 60_000 - (now - stamps.first())
                kotlinx.coroutines.delay(wait.coerceIn(200, 5_000))
            }
        }
    }

    companion object {
        private const val TAG = "GhostClient"
    }
}

/** Random padding header value. */
object Padding {
    fun value(): String = (1..8).map { ('a'..'z').random() }.joinToString("")
}

/**
 * Header interceptor re-ordering into Chrome's real order and adding sec-ch-ua hints.
 * No custom app header is ever sent, so nothing declares "an app did this".
 */
class HeaderOrderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val has = original.headers.names()
        if (!has.contains("User-Agent")) return chain.proceed(original)

        val builder = original.newBuilder()
        builder.removeHeader("Accept")
        builder.removeHeader("Accept-Language")
        builder.removeHeader("sec-ch-ua")
        builder.removeHeader("sec-ch-ua-mobile")
        builder.removeHeader("sec-ch-ua-platform")

        val ordered = buildList {
            add("User-Agent" to original.header("User-Agent")!!)
            original.header("sec-ch-ua")?.let { add("sec-ch-ua" to it) }
            add("sec-ch-ua-mobile" to "?1")
            original.header("sec-ch-ua-platform")?.let { add("sec-ch-ua-platform" to it) }
            add("Accept" to (original.header("Accept") ?: "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"))
            add("Accept-Language" to (original.header("Accept-Language") ?: "en-IN,en;q=0.9,en-US;q=0.8"))
            original.header("Accept-Encoding")?.let { add("Accept-Encoding" to it) }
            add("sec-fetch-site" to "none")
            add("sec-fetch-mode" to "navigate")
            add("sec-fetch-dest" to "document")
            original.header("Referer")?.let { add("Referer" to it) }
            original.header("Origin")?.let { add("Origin" to it) }
            original.header("Content-Type")?.let { add("Content-Type" to it) }
            original.header("X-Pad")?.let { add("X-Pad" to it) }
        }
        val rebuilt = original.newBuilder()
        ordered.forEach { (k, v) -> rebuilt.addHeader(k, v) }
        return chain.proceed(rebuilt.build())
    }
}

/** Credentials helper used when a proxy entry carries user:pass@host:port. */
object ProxyCredentials {
    fun header(user: String, pass: String): String = Credentials.basic(user, pass)
}
