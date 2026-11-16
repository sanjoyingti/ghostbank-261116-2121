package com.aibuilt.ghostbank.data

import com.aibuilt.ghostbank.model.EndpointState

/**
 * Fix 2 self-check. An endpoint may only claim VERIFIED when the recorded probe actually
 * returned a live 2xx/3xx and carries its evidence string.
 */
object RegistryAudit {

    fun violations(): List<String> {
        val out = ArrayList<String>()
        BankRegistry.banks.forEach { bank ->
            if (bank.endpoints.isEmpty()) out += "${bank.code}: bank has no endpoint at all"
            bank.endpoints.forEach { ep ->
                if (ep.state == EndpointState.VERIFIED) {
                    val status = ep.probeStatus
                    if (status == null || status !in 200..399) {
                        out += "${bank.code}/${ep.id}: VERIFIED without a live 2xx/3xx probe (status=$status)"
                    }
                    if (ep.probeEvidence.isBlank()) {
                        out += "${bank.code}/${ep.id}: VERIFIED without probe evidence"
                    }
                }
            }
        }
        val inventedDebit = BankRegistry.verifiedDebitEndpoints.any {
            it.urlTemplate.contains("/api/imps/p2a/debit")
        }
        if (inventedDebit) out += "invented /api/imps/p2a/debit is still in the verified set"
        if (BankRegistry.banks.size < 27) out += "only ${BankRegistry.banks.size} banks registered"
        if (BankRegistry.verifiedDebitEndpoints.isEmpty()) out += "no verified endpoint at all"
        return out
    }

    fun isClean(): Boolean = violations().isEmpty()
}
