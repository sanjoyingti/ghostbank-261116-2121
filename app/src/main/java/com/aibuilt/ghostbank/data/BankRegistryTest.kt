package com.aibuilt.ghostbank.data

import com.aibuilt.ghostbank.model.EndpointState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fix 2: no endpoint may be marked VERIFIED unless a live probe actually passed.
 * The invented SBI routes stay probe-only forever.
 */
class BankRegistryTest {

    @Test
    fun everyVerifiedEndpointHasAPassingProbe() {
        val verified = BankRegistry.banks.flatMap { it.verifiedEndpoints }
        assertTrue("registry must contain at least one verified endpoint", verified.isNotEmpty())
        verified.forEach { endpoint ->
            assertEquals(EndpointState.VERIFIED, endpoint.state)
            val status = endpoint.probeStatus
            assertNotNull("verified endpoint ${endpoint.id} has no probe status", status)
            assertTrue(
                "verified endpoint ${endpoint.id} probe status $status is not 2xx/3xx",
                status!! in 200..399
            )
            assertTrue("verified endpoint ${endpoint.id} has no probe evidence", endpoint.probeEvidence.isNotBlank())
        }
    }

    @Test
    fun inventedSbiRoutesAreProbeOnly() {
        val sbi = BankRegistry.byCode("SBIN")
        assertNotNull(sbi)
        val urls = sbi!!.endpoints.map { it.urlTemplate }
        assertFalse(urls.any { it.contains("/api/p2a/resolve") && sbi.verifiedEndpoints.any { e -> e.urlTemplate == it } })
        assertFalse(urls.any { it.contains("/api/imps/p2a/debit") && sbi.verifiedEndpoints.any { e -> e.urlTemplate == it } })
        assertTrue(sbi.endpoints.any { it.urlTemplate.contains("/api/p2a/resolve") })
        assertTrue(sbi.endpoints.any { it.urlTemplate.contains("/api/imps/p2a/debit") })
    }

    @Test
    fun verifiedDebitRegistryOnlyHoldsVerifiedEntries() {
        assertTrue(BankRegistry.verifiedDebitEndpoints.isNotEmpty())
        BankRegistry.verifiedDebitEndpoints.forEach {
            assertEquals(EndpointState.VERIFIED, it.state)
        }
        BankRegistry.probeOnlyEndpoints.forEach {
            assertEquals(EndpointState.UNVERIFIED, it.state)
        }
    }

    @Test
    fun ifscResolverIsTheVerifiedOne() {
        assertEquals("https://ifsc.razorpay.com/", BankRegistry.IFSC_RESOLVER)
    }

    @Test
    fun allBanksHavePrefixHomepageCapAndRail() {
        assertTrue(BankRegistry.banks.size >= 27)
        BankRegistry.banks.forEach { bank ->
            assertEquals(4, bank.code.length)
            assertTrue(bank.homepage.startsWith("https://"))
            assertTrue(bank.perTxCapPaise >= 100L)
            assertTrue(bank.endpoints.isNotEmpty())
        }
    }
}
