package com.aibuilt.ghostbank.data.net

import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.model.ResolveOutcome
import com.aibuilt.ghostbank.model.ResolveVerdict
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Fix 1: the resolve stage calls https://ifsc.razorpay.com/{IFSC} - the endpoint actually
 * verified live - and never invents a path again.
 *
 * 200 + BANK + IMPS=true  -> LIVE_SUPPORTED (bank name and branch go to the UI)
 * 200 + IMPS=false        -> FLOOR_SUPPORTED
 * 404                     -> INVALID_IFSC, hard fail, message "IFSC not found"
 * anything else           -> fallback probe against the bank's own registered route, else UNCONFIRMED
 */
class IfscResolver(private val client: GhostClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun resolve(ifsc: String, runId: String, profile: Map<String, String>): ResolveOutcome {
        val clean = ifsc.trim().uppercase()
        val url = BankRegistry.IFSC_RESOLVER + clean
        val started = System.currentTimeMillis()
        val walk = client.callOrNull(
            runId = runId,
            stage = "RESOLVE_PAYEE",
            url = url,
            headers = profile,
            method = "GET",
            body = null
        )
        val elapsed = System.currentTimeMillis() - started
        if (walk == null) {
            return ResolveOutcome(
                verdict = ResolveVerdict.UNCONFIRMED,
                httpStatus = null, bankName = null, branch = null,
                imps = null, upi = null, rtgs = null,
                body = "Transport failure: no exit reachable (kill-switch)",
                endpointUsed = url, hops = 0, finalUrl = url, elapsedMs = elapsed
            )
        }

        val status = walk.status
        if (status == 404) {
            return ResolveOutcome(
                verdict = ResolveVerdict.INVALID_IFSC,
                httpStatus = 404, bankName = null, branch = null,
                imps = null, upi = null, rtgs = null,
                body = walk.body.take(2000),
                endpointUsed = url, hops = walk.hops, finalUrl = walk.finalUrl, elapsedMs = elapsed
            )
        }

        if (status == 200) {
            val obj = runCatching { json.parseToJsonElement(walk.body) as? JsonObject }.getOrNull()
            val bank = obj?.get("BANK")?.let { contentOrNull(it) }
            if (obj != null && bank != null) {
                val branch = obj["BRANCH"]?.let { contentOrNull(it) }
                val imps = obj["IMPS"]?.jsonPrimitive?.booleanOrNull
                val upi = obj["UPI"]?.jsonPrimitive?.booleanOrNull
                val rtgs = obj["RTGS"]?.jsonPrimitive?.booleanOrNull
                return ResolveOutcome(
                    verdict = if (imps == true) ResolveVerdict.LIVE_SUPPORTED else ResolveVerdict.FLOOR_SUPPORTED,
                    httpStatus = 200,
                    bankName = bank,
                    branch = branch,
                    imps = imps,
                    upi = upi,
                    rtgs = rtgs,
                    body = walk.body.take(4000),
                    endpointUsed = url,
                    hops = walk.hops,
                    finalUrl = walk.finalUrl,
                    elapsedMs = elapsed
                )
            }
            return ResolveOutcome(
                verdict = ResolveVerdict.UNCONFIRMED,
                httpStatus = 200, bankName = null, branch = null,
                imps = null, upi = null, rtgs = null,
                body = walk.body.take(2000),
                endpointUsed = url, hops = walk.hops, finalUrl = walk.finalUrl, elapsedMs = elapsed
            )
        }

        // Non-200: probe the bank's own registered route if it has one, else UNCONFIRMED.
        val bank = BankRegistry.byIfscPrefix(clean)
        val fallback = bank?.endpoints?.firstOrNull {
            it.urlTemplate != BankRegistry.IFSC_RESOLVER + "{IFSC}"
        }
        if (bank != null && fallback != null) {
            val fbUrl = fallback.urlTemplate.replace("{IFSC}", clean)
            val fb = client.callOrNull(runId, "RESOLVE_PAYEE", fbUrl, profile, fallback.method, null)
            if (fb != null) {
                val fbBank = runCatching {
                    val o = json.parseToJsonElement(fb.body) as? JsonObject
                    o?.get("BANK")?.let { contentOrNull(it) }
                }.getOrNull()
                if (fb.status == 200) {
                    return ResolveOutcome(
                        verdict = ResolveVerdict.LIVE_SUPPORTED,
                        httpStatus = fb.status, bankName = fbBank ?: bank.name, branch = null,
                        imps = true, upi = null, rtgs = null,
                        body = fb.body.take(2000),
                        endpointUsed = fbUrl, hops = fb.hops, finalUrl = fb.finalUrl, elapsedMs = elapsed
                    )
                }
            }
        }

        return ResolveOutcome(
            verdict = ResolveVerdict.UNCONFIRMED,
            httpStatus = status, bankName = bank?.name, branch = null,
            imps = null, upi = null, rtgs = null,
            body = walk.body.take(2000),
            endpointUsed = url, hops = walk.hops, finalUrl = walk.finalUrl, elapsedMs = elapsed
        )
    }

    private fun contentOrNull(element: kotlinx.serialization.json.JsonElement): String? {
        val primitive = element as? JsonPrimitive ?: return null
        return if (primitive is kotlinx.serialization.json.JsonNull) null else primitive.content
    }
}
