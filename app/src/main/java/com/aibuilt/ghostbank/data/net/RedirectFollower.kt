package com.aibuilt.ghostbank.data.net

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Manual redirect walker: follows up to maxHops, keeps the method and the original headers on
 * every hop, and hands back the final URL plus the hop count so the log is honest.
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

    fun follow(client: OkHttpClient, original: Request): Walk {
        var hops = 0
        var current = original
        while (true) {
            client.newCall(current).execute().use { response ->
                val code = response.code
                val location = response.header("Location")
                if (code in hopCodes && !location.isNullOrBlank() && hops < maxHops) {
                    val nextUrl = if (location.startsWith("http")) {
                        location
                    } else {
                        current.url.resolve(location)?.toString() ?: location
                    }
                    hops++
                    current = nextHop(original, current, nextUrl, code)
                } else {
                    return Walk(
                        status = code,
                        body = response.body?.string().orEmpty(),
                        hops = hops,
                        finalUrl = current.url.toString(),
                        contentType = response.header("Content-Type"),
                        headers = response.headers.toString()
                    )
                }
            }
        }
    }

    /** Keeps the original method and every original header across the hop. */
    private fun nextHop(original: Request, current: Request, url: String, status: Int): Request {
        val builder = Request.Builder().url(url)
        original.headers.forEach { (name, value) -> builder.addHeader(name, value) }
        val body = current.body
        return when {
            body != null && status != 303 -> builder.method(current.method, body).build()
            body != null -> builder.method("GET", null).build()
            else -> builder.method(current.method, null).build()
        }
    }

    /** 2xx and 3xx are ALIVE. Only 4xx/5xx count as dead. */
    fun isAlive(status: Int): Boolean = status in 200..399

    @Throws(IOException::class)
    fun ping(client: OkHttpClient, url: String, headers: Map<String, String>): Walk {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        return follow(client, builder.build())
    }
}
