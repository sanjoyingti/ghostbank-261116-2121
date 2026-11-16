package com.aibuilt.ghostbank

import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.model.EndpointState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Registry honesty gate: an endpoint may only be VERIFIED when a live probe produced a
 * 2xx/3xx status and left evidence behind. Invented routes stay probe-only.
 */
class BankRegistryTest {

    @Test
    fun everyVerifiedEndpointCarriesLiveProof() {
        val verified = BankRegistry.banks
            .flatMap { bank -> bank.endpoints }
            .filter { it.state == EndpointState.VERIFIED }

        assertTrue("registry must contain verified endpoints", verified.isNotEmpty())

        verified.forEach { endpoint ->
            val status = endpoint.probeStatus
            assertNotNull("verified endpoint ${endpoint.id} has no probe status", status)
            assertTrue(
                "verified endpoint ${endpoint.id} status $status must be 2xx/3xx",
                status!! in 200..399
            )
            assertTrue(
                "verified endpoint ${endpoint.id} has no probe evidence",
                endpoint.probeEvidence.isNotBlank()
            )
            assertTrue(
                "verified endpoint ${endpoint.id} must be the proven IFSC resolver",
                endpoint.urlTemplate.startsWith(BankRegistry.IFSC_RESOLVER)
            )
        }
    }

    @Test
    fun inventedSbiPathsAreProbeOnly() {
        val sbi = BankRegistry.byCode("SBIN")
        assertNotNull(sbi)
        val ids = sbi!!.endpoints.map { it.id }
        assertTrue(ids.contains("sbi-p2a-resolve"))
        assertTrue(ids.contains("sbi-p2a-debit"))

        sbi.endpoints.filter { it.id.startsWith("sbi-p2a") }.forEach {
            assertEquals(
                "invented route ${it.id} must not be trusted",
                EndpointState.UNVERIFIED,
                it.state
            )
            assertEquals(404, it.probeStatus)
        }
    }

    @Test
    fun debitSetExcludesTheDeletedP2aDebitRoute() {
        val verifiedIds = BankRegistry.verifiedDebitEndpoints.map { it.id }
        assertTrue(verifiedIds.isNotEmpty())
        assertFalse(verifiedIds.contains("sbi-p2a-debit"))
        assertFalse(verifiedIds.contains("sbi-p2a-resolve"))
        BankRegistry.verifiedDebitEndpoints.forEach {
            assertEquals(EndpointState.VERIFIED, it.state)
        }
    }

    @Test
    fun registryCoversAllTwentySevenBanksWithCaps() {
        assertEquals(27, BankRegistry.banks.size)
        BankRegistry.banks.forEach { bank ->
            assertTrue("${bank.code} needs a positive cap", bank.perTxCapPaise > 0)
            assertTrue("${bank.code} needs a homepage", bank.homepage.startsWith("https://"))
        }
    }
}
