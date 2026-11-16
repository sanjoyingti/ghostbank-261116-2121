package com.aibuilt.ghostbank.data.net

import android.util.Log
import com.aibuilt.ghostbank.data.db.HostLogDao
import com.aibuilt.ghostbank.data.db.HostLogEntity
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.data.settings.ProxyEntry
import com.aibuilt.ghostbank.data.settings.SettingsStore
import com.aibuilt.ghostbank.model.HopResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    /**
     * Velocity gate. The lock only reserves a slot (no suspension inside the lock) and the
     * waiting happens outside it, which is what the coroutines library requires.
     */
    private suspend fun gate(cfg: GhostSettings) {
        while (true) {
            val waitMs = mutex.withLock { velocity.reserve(cfg) }
            if (waitMs <= 0L) return
            delay(waitMs.coerceIn(200L, 5_000L))
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
        gate(cfg)

        val proxy = proxyFor(cfg)
        val client = baseClient(proxy)

        var builder = Request.Builder().url(url)
        if (cfg.padding) builder = builder.addHeader("X-Pad", Padding.value())
        headers.forEach { (k, v) ->
            if (!k.equals("X-Pad", true)) builder = builder.addHeader(k, v)
        }
        val request = if (method == "POST" && body != null) {
            builder.post(body.toRequestBody(JSON_MEDIA)).build()
        } else {
            builder.get().build()
        }

        val started = System.currentTimeMillis()
        val walk = try {
            redirectFollower.follow(client, request)
        } catch (e: Exception) {
            Log.w(TAG, "call failed $url: ${e.message}")
            safeLog(runId, stage, method, url, url, -1, 0, System.currentTimeMillis() - started, headers, e.toString(), cfg)
            client.dispatcher.executorService.shutdown()
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

    /** Simple velocity window so we never exceed the configured requests/minute. */
    private class VelocityWindow {
        private val stamps = ArrayDeque<Long>()

        /** 0 when the slot is free, otherwise how long the caller must wait. Not suspending. */
        fun reserve(cfg: GhostSettings): Long {
            val perMinute = cfg.maxRequestsPerMinute.coerceAtLeast(1)
            val now = System.currentTimeMillis()
            while (stamps.isNotEmpty() && now - stamps.first() > 60_000L) stamps.removeFirst()
            if (stamps.size < perMinute) {
                stamps.addLast(now)
                return 0L
            }
            val oldest = stamps.first()
            return (60_000L - (now - oldest)).coerceAtLeast(1L)
        }
    }

    companion object {
        private const val TAG = "GhostClient"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
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
        val userAgent = original.header("User-Agent") ?: return chain.proceed(original)

        val builder = original.newBuilder()
        builder.removeHeader("Accept")
        builder.removeHeader("Accept-Language")
        builder.removeHeader("sec-ch-ua")
        builder.removeHeader("sec-ch-ua-mobile")
        builder.removeHeader("sec-ch-ua-platform")
        builder.removeHeader("sec-fetch-site")
        builder.removeHeader("sec-fetch-mode")
        builder.removeHeader("sec-fetch-dest")
        builder.removeHeader("User-Agent")
        builder.removeHeader("Referer")
        builder.removeHeader("Origin")
        builder.removeHeader("Content-Type")
        builder.removeHeader("X-Pad")

        builder.addHeader("User-Agent", userAgent)
        original.header("sec-ch-ua")?.let { builder.addHeader("sec-ch-ua", it) }
        builder.addHeader("sec-ch-ua-mobile", "?1")
        original.header("sec-ch-ua-platform")?.let { builder.addHeader("sec-ch-ua-platform", it) }
        builder.addHeader(
            "Accept",
            original.header("Accept")
                ?: "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
        )
        builder.addHeader(
            "Accept-Language",
            original.header("Accept-Language") ?: "en-IN,en;q=0.9,en-US;q=0.8"
        )
        original.header("Accept-Encoding")?.let { builder.addHeader("Accept-Encoding", it) }
        builder.addHeader("sec-fetch-site", "none")
        builder.addHeader("sec-fetch-mode", "navigate")
        builder.addHeader("sec-fetch-dest", "document")
        original.header("Referer")?.let { builder.addHeader("Referer", it) }
        original.header("Origin")?.let { builder.addHeader("Origin", it) }
        original.header("Content-Type")?.let { builder.addHeader("Content-Type", it) }
        original.header("X-Pad")?.let { builder.addHeader("X-Pad", it) }

        return chain.proceed(builder.build())
    }
}

/** Credentials helper used when a proxy entry carries user:pass@host:port. */
object ProxyCredentials {
    fun header(user: String, pass: String): String = Credentials.basic(user, pass)
}
