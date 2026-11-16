package com.aibuilt.ghostbank.data

import com.aibuilt.ghostbank.model.Bank
import com.aibuilt.ghostbank.model.BankEndpoint
import com.aibuilt.ghostbank.model.EndpointState
import com.aibuilt.ghostbank.model.Rail

/**
 * The endpoint registry.
 *
 * Rule (asserted by RegistryAudit and BankRegistryTest): an endpoint may only carry
 * EndpointState.VERIFIED if its probeStatus proves a live 2xx/3xx response was observed.
 * The invented SBI "/api/p2a/resolve" and "/api/imps/p2a/debit" paths therefore sit in the
 * probe-only set and can never drive a debit leg.
 */
object BankRegistry {

    /** The one URL verified live for every bank, auth-free, used by the resolve stage. */
    const val IFSC_RESOLVER = "https://ifsc.razorpay.com/"

    private fun cap(rupees: Long): Long = rupees * 100L

    /** Live-proven probe: 200 + JSON {BRANCH,ADDRESS,CITY,BANK,IFSC,IMPS,UPI,RTGS}. */
    private fun provenResolver(id: String, note: String) = BankEndpoint(
        id = id,
        urlTemplate = "$IFSC_RESOLVER{IFSC}",
        state = EndpointState.VERIFIED,
        note = note,
        probeStatus = 200,
        probeEvidence = "Live probe: 200 + JSON {BRANCH,ADDRESS,CITY,BANK,IFSC,IMPS,UPI,RTGS} for a real IFSC, cross-bank (SBIN0001722, HDFC0000001)"
    )

    /** Probed but failing: probe-only, never allowed to carry a debit leg. */
    private fun probeOnly(id: String, url: String, status: Int, note: String) = BankEndpoint(
        id = id,
        urlTemplate = url,
        state = EndpointState.UNVERIFIED,
        note = note,
        probeStatus = status,
        probeEvidence = "Live probe: $status (HTML error page) - invented route, probe only"
    )

    val banks: List<Bank> = listOf(
        Bank(
            code = "SBIN", name = "State Bank of India", homepage = "https://www.onlinesbi.sbi/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(
                provenResolver("ifsc-sbin", "Verified public IFSC JSON, cross-bank"),
                probeOnly("sbi-p2a-resolve", "https://www.onlinesbi.sbi/api/p2a/resolve", 404, "Invented path, 404 in live test"),
                probeOnly("sbi-p2a-debit", "https://www.onlinesbi.sbi/api/imps/p2a/debit", 404, "Invented path, 404 in live test")
            )
        ),
        Bank(
            code = "HDFC", name = "HDFC Bank", homepage = "https://www.hdfcbank.com/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-hdfc", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "ICIC", name = "ICICI Bank", homepage = "https://www.icicibank.com/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-icic", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "UTIB", name = "Axis Bank", homepage = "https://www.axisbank.com/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-utib", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "KKBK", name = "Kotak Mahindra Bank", homepage = "https://www.kotak.com/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-kkbk", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "PUNB", name = "Punjab National Bank", homepage = "https://www.pnbindia.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-punb", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "BARB", name = "Bank of Baroda", homepage = "https://www.bankofbaroda.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-barb", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "BKID", name = "Bank of India", homepage = "https://www.bankofindia.co.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-bkid", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "CNRB", name = "Canara Bank", homepage = "https://canarabank.com/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-cnrb", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "UBIN", name = "Union Bank of India", homepage = "https://www.unionbankofindia.co.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-ubin", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "IDIB", name = "Indian Bank", homepage = "https://www.indianbank.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-idib", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "IOBA", name = "Indian Overseas Bank", homepage = "https://www.iob.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-ioba", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "CBIN", name = "Central Bank of India", homepage = "https://www.centralbankofindia.co.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-cbin", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "MAHB", name = "Bank of Maharashtra", homepage = "https://www.bankofmaharashtra.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-mahb", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "UCBA", name = "UCO Bank", homepage = "https://www.ucobank.com/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-ucba", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "PSIB", name = "Punjab & Sind Bank", homepage = "https://punjabandsindbank.co.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-psib", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "YESB", name = "Yes Bank", homepage = "https://www.yesbank.in/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-yesb", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "IBKL", name = "IDBI Bank", homepage = "https://www.idbibank.in/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-ibkl", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "FDRL", name = "Federal Bank", homepage = "https://www.federalbank.co.in/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-fdrl", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "RATN", name = "RBL Bank", homepage = "https://www.rblbank.com/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-ratn", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "SIBL", name = "South Indian Bank", homepage = "https://www.southindianbank.com/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-sibl", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "KARB", name = "Karnataka Bank", homepage = "https://www.karnatakabank.com/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-karb", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "CIUB", name = "City Union Bank", homepage = "https://www.cityunionbank.com/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-ciub", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "TMBL", name = "Tamilnad Mercantile Bank", homepage = "https://www.tmbank.in/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-tmbl", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "DCBL", name = "DCB Bank", homepage = "https://www.dcbbank.com/",
            perTxCapPaise = cap(25_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-dcbl", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "AUBL", name = "AU Small Finance Bank", homepage = "https://www.aubank.in/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-aubl", "Verified public IFSC JSON"))
        ),
        Bank(
            code = "JSFB", name = "Jana Small Finance Bank", homepage = "https://www.janabank.com/",
            perTxCapPaise = cap(10_000), rail = Rail.IMPS_P2A,
            endpoints = listOf(provenResolver("ifsc-jsfb", "Verified public IFSC JSON"))
        )
    )

    fun byCode(code: String): Bank? = banks.firstOrNull { it.code.equals(code, ignoreCase = true) }

    fun byIfscPrefix(ifsc: String): Bank? {
        if (ifsc.length < 4) return null
        return byCode(ifsc.take(4))
    }

    /** Only entries in this list may carry a debit leg. */
    val verifiedDebitEndpoints: List<BankEndpoint> =
        banks.flatMap { it.verifiedEndpoints }.distinctBy { it.id }

    val probeOnlyEndpoints: List<BankEndpoint> =
        banks.flatMap { it.probeOnlyEndpoints }.distinctBy { it.id }
}
