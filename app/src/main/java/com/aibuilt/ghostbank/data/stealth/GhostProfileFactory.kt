package com.aibuilt.ghostbank.data.stealth

import kotlin.math.cos
import kotlin.math.ln
import kotlin.random.Random

/**
 * One identity per account, frozen at creation and burned after use. Two accounts never
 * share a fingerprint, and the permutation stays inside Chrome's valid header set.
 */
class GhostProfileFactory {

    private val chromeMajor = listOf(126, 127, 128, 129, 130)
    private val platforms = listOf(
        Triple("Android", "14", "SM-S911B"),
        Triple("Android", "13", "Pixel 7"),
        Triple("Android", "12", "Redmi Note 11"),
        Triple("Android", "14", "moto g84 5G"),
        Triple("Android", "15", "Pixel 8a")
    )
    private val languages = listOf("en-IN,en;q=0.9", "en-GB,en;q=0.9", "en-US,en;q=0.9,en-IN;q=0.8")

    data class Profile(
        val userAgent: String,
        val clientHints: String,
        val platform: String,
        val acceptLanguage: String,
        val headerOrder: List<String>,
        val jitterSeed: Long
    ) {
        /** Header map already in the profile's frozen order. */
        fun headers(referer: String? = null): Map<String, String> {
            val raw = linkedMapOf(
                "User-Agent" to userAgent,
                "sec-ch-ua" to clientHints,
                "sec-ch-ua-mobile" to "?1",
                "sec-ch-ua-platform" to "\"$platform\"",
                "Accept-Language" to acceptLanguage,
                "Accept-Encoding" to "gzip, deflate, br",
                "sec-fetch-site" to if (referer == null) "none" else "cross-site",
                "sec-fetch-mode" to "navigate",
                "sec-fetch-dest" to "document"
            )
            if (referer != null) raw["Referer"] = referer
            val out = LinkedHashMap<String, String>()
            headerOrder.forEach { name -> raw[name]?.let { out[name] = it } }
            raw.forEach { (k, v) -> if (!out.containsKey(k)) out[k] = v }
            return out
        }
    }

    fun fresh(runSeed: String): Profile {
        val rnd = Random(runSeed.hashCode().toLong())
        val major = chromeMajor.random(rnd)
        val (platform, version, model) = platforms.random(rnd)
        val build = rnd.nextInt(5200, 6400)
        val patch = rnd.nextInt(60, 220)
        val ua = "Mozilla/5.0 (Linux; Android $version; $model) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$major.0.$build.$patch Mobile Safari/537.36"
        val hints = "\"Chromium\";v=\"$major\", \"Google Chrome\";v=\"$major\", \"Not.A/Brand\";v=\"99\""
        val base = listOf(
            "User-Agent", "sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform",
            "Accept", "Accept-Language", "Accept-Encoding",
            "sec-fetch-site", "sec-fetch-mode", "sec-fetch-dest"
        )
        val ordered = base.shuffled(rnd)
        return Profile(
            userAgent = ua,
            clientHints = hints,
            platform = platform,
            acceptLanguage = languages.random(rnd),
            headerOrder = ordered,
            jitterSeed = rnd.nextLong()
        )
    }
}

/** Poisson-distributed waits so gaps cluster like a person, not a cron. */
object PoissonJitter {
    fun nextMillis(meanMs: Long, seed: Long = System.nanoTime()): Long {
        val rnd = Random(seed)
        val u = rnd.nextDouble().coerceIn(1e-9, 0.9999999)
        val x = -ln(1.0 - u) * meanMs
        return x.toLong().coerceIn(120L, meanMs * 6)
    }
}

/** Diurnal shaping: slower at night, faster in the working day. */
object Diurnal {
    fun factorFor(hourOfDay: Int): Double {
        return when (hourOfDay) {
            in 9..22 -> 1.0
            23 -> 1.8
            in 0..6 -> 2.6
            else -> 1.4
        }
    }

    fun inWindow(hourOfDay: Int, start: Int, end: Int): Boolean {
        if (start <= end) return hourOfDay in start until end
        return hourOfDay >= start || hourOfDay < end
    }

    /** Smooth cosine curve so the 09:00 and 23:00 edges are not a cliff. */
    fun edgeSoftening(hourOfDay: Int): Double {
        val angle = (hourOfDay / 24.0) * 2 * Math.PI
        return 0.85 + 0.15 * (1 + cos(angle)) / 2
    }
}
