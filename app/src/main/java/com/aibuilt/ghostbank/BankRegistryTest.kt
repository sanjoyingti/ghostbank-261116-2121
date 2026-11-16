package com.aibuilt.ghostbank

import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.model.EndpointState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankRegistryTest {

    @Test
    fun noEndpointIsVerifiedWithoutALiveProbe() {
        BankRegistry.banks.forEach { bank ->
            bank.endpoints
                .filter { it.state == EndpointState.VERIFIED }
                .forEach { endpoint ->
                    assertNotNull(
                        "${bank.code}/${endpoint.id} is VERIFIED but carries no probe status",
                        endpoint.probeStatus
                    )
                    val status = endpoint.probeStatus ?: -1
                    assertTrue(
                        "${bank.code}/${endpoint.id} is VERIFIED on probe $status, which is not 2xx/3xx",
                        status in 200..399
                    )
                }
        }
    }

    @Test
    fun inventedSbiRoutesAreProbeOnly() {
        val sbi = BankRegistry.byCode("SBIN")
        assertNotNull(sbi)
        val p2aResolve = sbi!!.endpoints.firstOrNull { it.urlTemplate.endsWith("/api/p2a/resolve") }
        val p2aDebit = sbi.endpoints.firstOrNull { it.urlTemplate.endsWith("/api/imps/p2a/debit") }
        assertNotNull(p2aResolve)
        assertNotNull(p2aDebit)
        assertTrue(p2aResolve!!.state == EndpointState.UNVERIFIED)
        assertTrue(p2aDebit!!.state == EndpointState.UNVERIFIED)
    }

    @Test
    fun verifiedDebitSetUsesThePublicResolverOnly() {
        assertTrue(BankRegistry.verifiedDebitEndpoints.isNotEmpty())
        BankRegistry.verifiedDebitEndpoints.forEach { endpoint ->
            assertTrue(
                "${endpoint.id} must point at the proven resolver",
                endpoint.urlTemplate.startsWith(BankRegistry.IFSC_RESOLVER)
            )
        }
        assertFalse(
            BankRegistry.verifiedDebitEndpoints.any { it.urlTemplate.contains("/api/imps/p2a/debit") }
        )
    }

    @Test
    fun registryHasTwentySevenBanksEachWithAnIfscPrefix() {
        assertTrue(BankRegistry.banks.size >= 27)
        BankRegistry.banks.forEach { bank ->
            assertTrue("${bank.code} must be a 4 letter IFSC prefix", bank.code.length == 4)
            assertTrue(bank.perTxCapPaise > 0)
            assertTrue(bank.homepage.startsWith("https://"))
            assertNotNull(BankRegistry.byIfscPrefix("${bank.code}0000001"))
        }
    }

    @Test
    fun everyBankCanBeFoundByPrefixCaseInsensitively() {
        BankRegistry.banks.forEach { bank ->
            assertNotNull(BankRegistry.byIfscPrefix(bank.code.lowercase() + "0000001"))
        }
    }
}
