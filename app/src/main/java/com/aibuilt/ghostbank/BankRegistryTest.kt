package com.aibuilt.ghostbank

import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.model.EndpointState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fix 2: no endpoint may be marked VERIFIED without a live probe that actually passed.
 */
class BankRegistryTest {

    @Test
    fun everyVerifiedEndpointCarriesLiveProbeEvidence() {
        BankRegistry.banks.forEach { bank ->
            bank.verifiedEndpoints.forEach { endpoint ->
                assertNotNull("${endpoint.id} is VERIFIED without a probe status", endpoint.probeStatus)
                val status = endpoint.probeStatus ?: -1
                assertTrue("${endpoint.id} is VERIFIED but probed $status", status in 200..399)
                assertTrue(
                    "${endpoint.id} is VERIFIED without evidence text",
                    endpoint.probeEvidence.isNotBlank()
                )
            }
        }
    }

    @Test
    fun inventedSbiRoutesAreProbeOnly() {
        val sbi = BankRegistry.byCode("SBIN")
        assertNotNull(sbi)
        val verifiedUrls = sbi!!.verifiedEndpoints.map { it.urlTemplate }
        assertFalse(verifiedUrls.any { it.contains("/api/p2a/resolve") })
        assertFalse(verifiedUrls.any { it.contains("/api/imps/p2a/debit") })
        val probeOnlyUrls = sbi.probeOnlyEndpoints.map { it.urlTemplate }
        assertTrue(probeOnlyUrls.any { it.contains("/api/p2a/resolve") })
        assertTrue(probeOnlyUrls.any { it.contains("/api/imps/p2a/debit") })
    }

    @Test
    fun noEndpointIsVerifiedWithoutAnHttpProbe() {
        BankRegistry.banks.flatMap { it.verifiedEndpoints }.forEach { endpoint ->
            assertEquals(EndpointState.VERIFIED, endpoint.state)
            assertTrue((endpoint.probeStatus ?: -1) in 200..399)
        }
    }

    @Test
    fun registryCoversTwentySevenBanksAndAllHaveAVerifiedResolver() {
        assertEquals(27, BankRegistry.banks.size)
        BankRegistry.banks.forEach { bank ->
            assertTrue("${bank.code} has no verified endpoint", bank.verifiedEndpoints.isNotEmpty())
            assertTrue("${bank.code} has no positive cap", bank.perTxCapPaise > 0)
            assertTrue("${bank.code} has no homepage", bank.homepage.startsWith("https://"))
        }
    }

    @Test
    fun everyBankLooksUpByItsIfscPrefix() {
        BankRegistry.banks.forEach { bank ->
            assertEquals(bank.code, BankRegistry.byIfscPrefix("${bank.code}0001722")?.code)
        }
    }
}
