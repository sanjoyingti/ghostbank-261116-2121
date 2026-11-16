package com.aibuilt.ghostbank.data.net

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Resolves names over Cloudflare's JSON DoH endpoint so the ISP resolver never sees bank hosts.
 * Falls back to the system resolver only if the DoH call itself fails.
 */
class DohDns : Dns {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val cache = HashMap<String, List<InetAddress>>()

    override fun lookup(hostname: String): List<InetAddress> {
        cache[hostname]?.let { return it }
        val resolved = runCatching { dohLookup(hostname) }.getOrNull()
        if (!resolved.isNullOrEmpty()) {
            cache[hostname] = resolved
            return resolved
        }
        return Dns.SYSTEM.lookup(hostname)
    }

    private fun dohLookup(hostname: String): List<InetAddress> {
        val request = Request.Builder()
            .url("https://cloudflare-dns.com/dns-query?name=$hostname&type=A")
            .header("Accept", "application/dns-json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: throw UnknownHostException(hostname)
            val root = json.parseToJsonElement(text) as? JsonObject ?: throw UnknownHostException(hostname)
            val answers = root["Answer"]?.jsonArray ?: return emptyList()
            val out = ArrayList<InetAddress>()
            for (entry in answers) {
                val obj = entry.jsonObject
                val type = obj["type"]?.jsonPrimitive?.content
                if (type != "1") continue
                val data = obj["data"]?.jsonPrimitive?.content ?: continue
                runCatching { out.add(InetAddress.getByName(data)) }
            }
            if (out.isEmpty()) Log.d("DohDns", "no A records for $hostname")
            return out
        }
    }
}
