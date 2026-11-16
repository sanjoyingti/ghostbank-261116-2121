package com.aibuilt.ghostbank.data.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Fix 3: Android's OkHttp will follow redirects on its own, but we need the hop count
 * and the final URL for the log, and we must keep the method and headers across hops.
 * OkHttp only replays headers on same-host redirects, so this follower walks hops manually.
 */
class RedirectFollower(private val maxHops: Int = 5) {

    data class Walk(
        val status: Int,
        val body: String,
        val hops: Int,
        val finalUrl: String,
        val contentType: String?,
        val headers: String
    )

    private val hopCodes = setOf(301, 302, 303, 307, 308)

    fun follow(client: OkHttpClient, request: Request): Walk {
        var hops = 0
        var current = request
        var response: Response? = null
        try {
            while (true) {
                val call = client.newCall(current)
                response?.close()
                val r = call.execute()
                response = r
                val location = r.header("Location")
                val code = r.code
                if (code in hopCodes && location != null && hops < maxHops) {
                    val next = if (location.startsWith("http")) {
                        location
                    } else {
                        current.url.resolve(location)?.toString() ?: location
                    }
                    // Preserve method and all original headers on every hop.
                    var builder = Request.Builder()
                        .url(next)
                    request.headers.forEach { (name, value) -> builder = builder.addHeader(name, value) }
                    val body = current.body
                    if (body != null && code != 303) {
                        builder = builder.method(current.method, body)
                    } else if (body != null) {
                        builder = builder.method("GET", null)
                    } else {
                        builder = builder.method(current.method, null)
                    }
                    current = builder.build()
                    hops++
                    continue
                }

                val text = r.body?.string().orEmpty()
                return Walk(
                    status = code,
                    body = text,
                    hops = hops,
                    finalUrl = current.url.toString(),
                    contentType = r.header("Content-Type"),
                    headers = r.headers.toString()
                )
            }
        } finally {
            response?.close()
        }
    }

    /** 2xx and 3xx are ALIVE. Only 4xx/5xx count as dead. */
    fun isAlive(status: Int): Boolean = status in 200..399

    @Throws(IOException::class)
    fun ping(client: OkHttpClient, url: String, headers: Map<String, String>): Walk {
        var builder = Request.Builder().url(url).get()
        headers.forEach { (k, v) -> builder = builder.addHeader(k, v) }
        return follow(client, builder.build())
    }
}
