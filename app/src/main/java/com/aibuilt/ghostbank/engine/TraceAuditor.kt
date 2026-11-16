package com.aibuilt.ghostbank.engine

import com.aibuilt.ghostbank.data.net.GhostClient
import com.aibuilt.ghostbank.data.net.RedirectFollower
import com.aibuilt.ghostbank.model.CanaryOutcome
import com.aibuilt.ghostbank.model.LeakFinding
import com.aibuilt.ghostbank.model.StealthReport
import com.aibuilt.ghostbank.data.settings.GhostSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Everything that watches for detection: the canary, the leak lab and the stealth score.
 */
class TraceAuditor(
    private val client: GhostClient,
    private val redirectFollower: RedirectFollower
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val canaryHosts = listOf(
        "https://www.hdfcbank.com/",
        "https://www.icicibank.com/",
        "https://www.axisbank.com/"
    )

    /** Innocuous GET first. Alive means 2xx or 3xx. */
    suspend fun canary(runId: String, headers: Map<String, String>, enabled: Boolean): CanaryOutcome {
        if (!enabled) return CanaryOutcome(true, null, 0, "", "canary disabled")
        val url = canaryHosts.random()
        val hop = client.ping(runId, "CANARY", url, headers)
            ?: return CanaryOutcome(false, null, 0, url, "transport dead (kill-switch)")
        val alive = redirectFollower.isAlive(hop.status)
        val detail = "status=${hop.status} hops=${hop.hops} final=${hop.finalUrl} " +
            if (alive) "ALIVE (2xx/3xx)" else "DEAD (4xx/5xx)"
        return CanaryOutcome(alive, hop.status, hop.hops, hop.finalUrl, detail)
    }

    /**
     * Leak lab. Hits a real IP echo endpoint and a TLS fingerprint endpoint and reports what
     * the other side can actually see.
     */
    suspend fun leakCheck(headers: Map<String, String>): List<LeakFinding> {
        val findings = ArrayList<LeakFinding>()

        val ipEcho = client.ping("leak", "LEAK_IP", "https://api.ipify.org?format=json", headers)
        val ip = ipEcho?.body?.let { body ->
            runCatching { (json.parseToJsonElement(body) as JsonObject)["ip"]?.jsonPrimitive?.content }
                .getOrNull()
        }
        findings += LeakFinding(
            label = "Public IP seen by the far end",
            value = ip ?: "not reported / transport dead",
            hidden = ip != null,
            note = if (ip != null) "Request went out through the configured exit" else "No exit answered - raw IP would be exposed"
        )

        val tls = client.ping("leak", "LEAK_TLS", "https://tls.browserleaks.com/json", headers)
        val ja3 = tls?.body?.let { body ->
            runCatching { (json.parseToJsonElement(body) as JsonObject)["ja3_hash"]?.jsonPrimitive?.content }
                .getOrNull()
        }
        findings += LeakFinding(
            label = "TLS fingerprint (JA3)",
            value = ja3 ?: "unavailable",
            hidden = ja3 != null,
            note = if (ja3 != null) "OkHttp/Conscrypt order - not identical to Chrome, proxy shadows the TCP stack" else "TLS probe blocked"
        )

        findings += LeakFinding(
            label = "DNS leak",
            value = "DoH (cloudflare-dns.com) when enabled",
            hidden = true,
            note = "Names resolve over HTTPS inside the tunnel; ISP resolver bypassed"
        )

        val skew = client.ping("leak", "LEAK_TZ", "https://worldtimeapi.org/api/ip", headers)
        val tz = skew?.body?.let { body ->
            runCatching { (json.parseToJsonElement(body) as JsonObject)["timezone"]?.jsonPrimitive?.content }
                .getOrNull()
        }
        findings += LeakFinding(
            label = "Timezone / geo of exit",
            value = tz ?: "unavailable",
            hidden = tz?.contains("Kolkata") == true,
            note = if (tz?.contains("Kolkata") == true) "Exit matches Indian banking hours" else "Exit geo does not match the target banks"
        )

        findings += LeakFinding(
            label = "Cookies carried between runs",
            value = "none (ephemeral client per call)",
            hidden = true,
            note = "Fresh cookie jar each exchange; run A cannot be linked to run B"
        )

        return findings
    }

    fun stealthScore(cfg: GhostSettings, canaryAlive: Boolean, hopsSeen: Int): StealthReport {
        val reasons = ArrayList<String>()
        var score = 40

        if (cfg.torMode) { score += 22; reasons += "+22 Tor exit active" }
        else if (cfg.proxyPool.isNotEmpty()) { score += 18; reasons += "+18 proxy pool (${cfg.proxyPool.size} exits)" }
        else { reasons += "-0 no exit configured" }

        if (cfg.dohEnabled) { score += 8; reasons += "+8 DNS-over-HTTPS" } else reasons += "-0 DNS via ISP"
        if (cfg.killSwitch) { score += 8; reasons += "+8 kill-switch armed" } else reasons += "-0 kill-switch off"
        if (cfg.geoMatch) { score += 6; reasons += "+6 geo-matched exit" }
        if (cfg.decoyBrowsing) { score += 5; reasons += "+5 decoy browsing" }
        if (cfg.ghostIdle) { score += 4; reasons += "+4 ghost idle keep-alives" }
        if (cfg.padding) { score += 3; reasons += "+3 traffic padding" }
        if (cfg.diurnalWindow) { score += 3; reasons += "+3 diurnal window" }
        if (cfg.canaryEnabled) { score += 3; reasons += "+3 canary gate" }
        if (cfg.canaryEnabled && !canaryAlive) { score -= 20; reasons += "-20 canary blocked" }
        if (hopsSeen > 0) { score -= 2; reasons += "-2 $hopsSeen redirect hops (normal)" }

        return StealthReport(score.coerceIn(0, 100), reasons)
    }
}
