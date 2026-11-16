package com.aibuilt.ghostbank.data

import com.aibuilt.ghostbank.model.BankEndpoint
import com.aibuilt.ghostbank.model.EndpointState

/**
 * Fix 2 in inspectable form: the same rule the unit test enforces, surfaced to the UI so the
 * claim "nothing is verified without a live probe" can be checked on the phone.
 */
object RegistryAudit {

    data class Violation(val bankCode: String, val endpointId: String, val reason: String)

    fun violations(): List<Violation> = BankRegistry.banks.flatMap { bank ->
        bank.endpoints.mapNotNull { endpoint ->
            when {
                endpoint.state == EndpointState.VERIFIED && endpoint.probeStatus == null ->
                    Violation(bank.code, endpoint.id, "marked VERIFIED with no probe status at all")
                endpoint.state == EndpointState.VERIFIED && endpoint.probeStatus !in 200..399 ->
                    Violation(bank.code, endpoint.id, "marked VERIFIED but last probe was ${endpoint.probeStatus}")
                else -> null
            }
        }
    }

    val ok: Boolean get() = violations().isEmpty()

    fun verifiedCount(): Int = BankRegistry.banks.sumOf { bank -> bank.verifiedEndpoints.size }

    fun probeOnlyCount(): Int = BankRegistry.banks.sumOf { bank -> bank.probeOnlyEndpoints.size }

    fun bannedRouteInVerifiedSet(): Boolean =
        BankRegistry.verifiedDebitEndpoints.any { it.urlTemplate.contains("/api/imps/p2a/debit") }

    /** Endpoints a debit leg may actually use for this bank. */
    fun debitEndpointsFor(bankCode: String): List<BankEndpoint> =
        BankRegistry.byCode(bankCode)?.verifiedEndpoints.orEmpty()

    fun describe(): String = buildString {
        appendLine("Registry rule: VERIFIED only with a live 2xx/3xx probe, else probe-only.")
        appendLine("Banks: ${BankRegistry.banks.size}")
        appendLine("Verified endpoints: ${verifiedCount()}")
        appendLine("Probe-only endpoints: ${probeOnlyCount()}")
        appendLine("Invented /api/imps/p2a/debit still verified: ${bannedRouteInVerifiedSet()}")
        if (ok) appendLine("Self-check: PASS") else appendLine("Self-check: FAIL")
        violations().forEach { appendLine("VIOLATION ${it.bankCode} ${it.endpointId} - ${it.reason}") }
    }
}
