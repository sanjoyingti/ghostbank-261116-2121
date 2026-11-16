package com.aibuilt.ghostbank

import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.model.EndpointState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fix 2: an endpoint may only be VERIFIED when a live probe passed. If someone invents a path
 * and marks it live, this test fails.
 *
 * The test lives in the main source set, so JUnit is a normal `implementation` dependency and
 * is packaged with the app rather than being test-only.
 */
class BankRegistryTest {

    @Test
    fun everyVerifiedEndpointCarriesProbeEvidence() {
        BankRegistry.banks.forEach { bank ->
            bank.endpoints
                .filter { it.state == EndpointState.VERIFIED }
                .forEach { endpoint ->
                    val status = endpoint.probeStatus
                    assertTrue(
                        "${bank.code}/${endpoint.id} is VERIFIED without a probe status",
                        status != null && status in 200..399
                    )
                    assertTrue(
                        "${bank.code}/${endpoint.id} is VERIFIED without evidence text",
                        endpoint.probeEvidence.isNotBlank()
                    )
                }
        }
    }

    @Test
    fun inventedSbiRoutesAreNotVerified() {
        val sbi = BankRegistry.byCode("SBIN")!!
        val invented = sbi.endpoints.filter {
            it.urlTemplate.contains("/api/p2a/resolve") || it.urlTemplate.contains("/api/imps/p2a/debit")
        }
        assertFalse(invented.isEmpty())
        invented.forEach { endpoint ->
            assertEquals(
                "${endpoint.id} still claims to be VERIFIED",
                EndpointState.UNVERIFIED,
                endpoint.state
            )
        }
    }

    @Test
    fun resolverIsTheVerifiedOne() {
        val verified = BankRegistry.verifiedDebitEndpoints
        assertTrue(verified.any { it.urlTemplate.startsWith(BankRegistry.IFSC_RESOLVER) })
        assertFalse(verified.any { it.urlTemplate.contains("/api/imps/p2a/debit") })
    }

    @Test
    fun allTwentySevenBanksCarryACap() {
        assertEquals(27, BankRegistry.banks.size)
        BankRegistry.banks.forEach { bank ->
            assertTrue("${bank.code} has no cap", bank.perTxCapPaise > 0)
            assertTrue("${bank.code} has no homepage", bank.homepage.startsWith("https://"))
        }
    }

    @Test
    fun verifiedEndpointsAreHttpsOnly() {
        BankRegistry.verifiedDebitEndpoints.forEach { endpoint ->
            assertTrue(
                "${endpoint.id} is verified but not https",
                endpoint.urlTemplate.startsWith("https://")
            )
        }
    }
}
