package com.aibuilt.ghostbank.model

import kotlinx.serialization.Serializable

/** Confidence that an endpoint URL actually exists on the live internet. */
@Serializable
enum class EndpointState { VERIFIED, UNVERIFIED }

/** One HTTP route belonging to a bank, with the probe verdict that backs it. */
@Serializable
data class BankEndpoint(
    val id: String,
    val urlTemplate: String,
    val state: EndpointState,
    val method: String = "GET",
    val note: String = "",
    val probeStatus: Int? = null,
    val probeEvidence: String = ""
)

/** A bank plus its caps, its site and any endpoints we are allowed to trust. */
data class Bank(
    val code: String,
    val name: String,
    val homepage: String,
    val perTxCapPaise: Long,
    val rail: Rail,
    val endpoints: List<BankEndpoint>,
    val redirectsAllowed: Boolean = true
) {
    val verifiedEndpoints: List<BankEndpoint>
        get() = endpoints.filter { it.state == EndpointState.VERIFIED }
    val probeOnlyEndpoints: List<BankEndpoint>
        get() = endpoints.filter { it.state == EndpointState.UNVERIFIED }
}

enum class Rail(val label: String) {
    IMPS_P2A("IMPS P2A"),
    UPI_VPA("UPI VPA"),
    NEFT("NEFT")
}

/** Why a resolve did or did not work, straight from the wire. */
enum class ResolveVerdict {
    LIVE_SUPPORTED,
    FLOOR_SUPPORTED,
    INVALID_IFSC,
    UNCONFIRMED
}

/** Per-account harvest state machine. */
enum class HarvestState {
    PENDING,
    SENT,
    SETTLED,
    FLOOR,
    REJECTED,
    HARVESTED
}

/** Pipeline stage order, used for ticks and timings. */
enum class Stage {
    INITIATED,
    FINGERPRINT,
    RESOLVE_PAYEE,
    CANARY,
    DEBIT_LEG,
    CREDIT_LEG,
    SETTLE,
    BOOKED
}

enum class StageResult { RUNNING, OK, FLOOR, FAILED, SKIPPED }

enum class LedgerDirection { DEBIT, CREDIT }

enum class StealthLevel { STANDARD, DEEP, GHOST }

data class ResolveOutcome(
    val verdict: ResolveVerdict,
    val httpStatus: Int?,
    val bankName: String?,
    val branch: String?,
    val imps: Boolean?,
    val upi: Boolean?,
    val rtgs: Boolean?,
    val body: String,
    val endpointUsed: String,
    val hops: Int,
    val finalUrl: String,
    val elapsedMs: Long
)

data class HopResult(
    val status: Int,
    val body: String,
    val hops: Int,
    val finalUrl: String,
    val contentType: String?
)

data class DebitOutcome(
    val ok: Boolean,
    val status: Int?,
    val body: String,
    val utr: String?,
    val endpointUsed: String,
    val hops: Int,
    val finalUrl: String,
    val elapsedMs: Long
)

data class CanaryOutcome(
    val alive: Boolean,
    val status: Int?,
    val hops: Int,
    val finalUrl: String,
    val detail: String
)

data class StageTick(
    val stage: Stage,
    val result: StageResult,
    val startedAt: Long,
    val endedAt: Long? = null,
    val detail: String = ""
) {
    val durationMs: Long get() = (endedAt ?: startedAt) - startedAt
}

data class StealthReport(
    val score: Int,
    val reasons: List<String>
)

data class LeakFinding(
    val label: String,
    val value: String,
    val hidden: Boolean,
    val note: String
)

data class VirtualAccount(
    val id: Long = 0,
    val accountNumber: String,
    val ifsc: String,
    val bankCode: String,
    val label: String,
    val uses: Int = 0,
    val active: Boolean = true
)
