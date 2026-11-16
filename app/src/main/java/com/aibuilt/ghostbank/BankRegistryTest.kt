package com.aibuilt.ghostbank

import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.data.RegistryAudit
import com.aibuilt.ghostbank.model.EndpointState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BankRegistryTest {

    @Test
    fun noEndpointIsVerifiedWithoutALiveProbe() {
        assertEquals(emptyList<String>(), RegistryAudit.violations())
    }

    @Test
    fun inventedSbiDebitRouteIsNotVerified() {
        val sbi = BankRegistry.byCode("SBIN")
        val debit = sbi?.endpoints?.firstOrNull { it.urlTemplate.contains("/api/imps/p2a/debit") }
        assertTrue(debit != null && debit.state == EndpointState.UNVERIFIED)
    }

    @Test
    fun resolveEndpointIsTheVerifiedIfscResolver() {
        for (bank in BankRegistry.banks) {
            for (ep in bank.verifiedEndpoints) {
                assertTrue(ep.urlTemplate.startsWith(BankRegistry.IFSC_RESOLVER))
                assertTrue(ep.probeStatus != null && ep.probeStatus!! in 200..399)
            }
        }
    }

    @Test
    fun allTwentySevenBanksHaveACap() {
        assertEquals(27, BankRegistry.banks.size)
        assertTrue(BankRegistry.banks.all { it.perTxCapPaise >= 100L })
    }
}
