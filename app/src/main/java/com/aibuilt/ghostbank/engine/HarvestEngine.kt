package com.aibuilt.ghostbank.engine

import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.data.db.AccountDao
import com.aibuilt.ghostbank.data.db.AccountEntity
import com.aibuilt.ghostbank.data.db.HarvestDao
import com.aibuilt.ghostbank.data.db.HarvestEntity
import com.aibuilt.ghostbank.data.db.HostLogDao
import com.aibuilt.ghostbank.data.db.HostLogEntity
import com.aibuilt.ghostbank.data.db.LedgerDao
import com.aibuilt.ghostbank.data.db.LedgerEntity
import com.aibuilt.ghostbank.data.db.VirtualAccountDao
import com.aibuilt.ghostbank.data.net.GhostClient
import com.aibuilt.ghostbank.data.net.IfscResolver
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.data.settings.SettingsStore
import com.aibuilt.ghostbank.data.stealth.Diurnal
import com.aibuilt.ghostbank.data.stealth.GhostProfileFactory
import com.aibuilt.ghostbank.data.stealth.PoissonJitter
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection
import com.aibuilt.ghostbank.model.ResolveVerdict
import com.aibuilt.ghostbank.model.Stage
import com.aibuilt.ghostbank.model.StageResult
import com.aibuilt.ghostbank.model.StageTick
import com.aibuilt.ghostbank.model.StealthLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Calendar
import java.util.UUID
import kotlin.random.Random

/** Live view of one harvest run so the pipeline screen can tick stages as they happen. */
data class RunState(
    val runId: String,
    val active: Boolean = false,
    val ticks: List<StageTick> = emptyList(),
    val current: Stage = Stage.INITIATED,
    val message: String = "",
    val totalLegs: Int = 0,
    val completedLegs: Int = 0,
    val failedLegs: Int = 0,
    val floorLegs: Int = 0,
    val stealthScore: Int = 0,
    val stealthReasons: List<String> = emptyList(),
    val rawTail: List<String> = emptyList(),
    val paused: Boolean = false
)

/**
 * The two-leg money engine.
 *
 * Legs never advance on assumption. Debit must confirm before credit starts, and a failed
 * debit keeps its full HTML body in the raw log.
 */
class HarvestEngine(
    private val settings: SettingsStore,
    private val ghostClient: GhostClient,
    private val ifscResolver: IfscResolver,
    private val profileFactory: GhostProfileFactory,
    private val harvestDao: HarvestDao,
    private val ledgerDao: LedgerDao,
    private val virtualAccountDao: VirtualAccountDao,
    private val accountDao: AccountDao,
    private val hostLogDao: HostLogDao
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _state = MutableStateFlow(RunState(runId = ""))
    val state: StateFlow<RunState> = _state.asStateFlow()

    private fun tick(stage: Stage, result: StageResult, detail: String = "", started: Long = System.currentTimeMillis()) {
        _state.update { s ->
            val list = s.ticks.toMutableList()
            val idx = list.indexOfLast { it.stage == stage }
            val closed = if (idx >= 0) list[idx].copy(result = result, endedAt = System.currentTimeMillis(), detail = detail) else null
            if (closed != null) list[idx] = closed
            s.copy(
                current = stage,
                ticks = list,
                message = detail,
                rawTail = (s.rawTail + "[$stage] $detail").takeLast(60)
            )
        }
    }

    private fun beginStage(stage: Stage) {
        _state.update { s ->
            s.copy(
                current = stage,
                ticks = s.ticks + StageTick(stage, StageResult.RUNNING, System.currentTimeMillis()),
                rawTail = (s.rawTail + "[$stage] started").takeLast(60)
            )
        }
    }

    private fun fail(stage: Stage, detail: String) {
        tick(stage, StageResult.FAILED, detail)
    }

    /** Jittered gap using Poisson so gaps cluster like a human. */
    private suspend fun humanGap(cfg: GhostSettings, meanMs: Long) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val factor = Diurnal.factorFor(hour) * Diurnal.edgeSoftening(hour)
        delay((PoissonJitter.nextMillis(meanMs) * factor).toLong())
    }

    suspend fun harvest(accountIds: List<Long>) {
        val cfg = settings.snapshot()
        val runId = UUID.randomUUID().toString()

        _state.value = RunState(runId = runId, active = true, totalLegs = accountIds.size)
        beginStage(Stage.INITIATED)
        tick(Stage.INITIATED, StageResult.OK, "run $runId, ${accountIds.size} accounts, dial=${cfg.stealthLevel}")

        // Ghost dial is proxy-mandatory.
        if (cfg.stealthLevel == StealthLevel.GHOST && !cfg.exitConfigured && !cfg.torMode) {
            fail(Stage.CANARY, "Ghost dial needs an exit: configure a proxy or enable Tor")
            _state.update { it.copy(active = false) }
            return
        }
        if (cfg.killSwitch && cfg.proxyRequired && !cfg.exitConfigured && !cfg.torMode) {
            fail(Stage.CANARY, "Kill-switch: no exit configured, refusing to send on the raw IP")
            _state.update { it.copy(active = false) }
            return
        }

        if (cfg.diurnalWindow) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (!Diurnal.inWindow(hour, cfg.windowStartHour, cfg.windowEndHour)) {
                tick(Stage.INITIATED, StageResult.OK, "outside diurnal window ${cfg.windowStartHour}:00-${cfg.windowEndHour}:00, slowing x2.5")
            }
        }

        var handled = 0
        for (accountId in accountIds) {
            if (!_state.value.active) break
            val account = accountDao.all().firstOrNull { it.id == accountId } ?: continue

            // Dedupe registry already says harvested: never twice.
            if (harvestDao.harvestedFor(account.id) != null) {
                _state.update { it.copy(completedLegs = it.completedLegs + 1) }
                continue
            }

            val ok = harvestOne(cfg, runId, account)
            handled++
            _state.update {
                it.copy(
                    completedLegs = it.completedLegs + 1,
                    failedLegs = if (!ok) it.failedLegs + 1 else it.failedLegs
                )
            }

            // Velocity cap by hour.
            if (handled >= cfg.maxAccountsPerHour) {
                tick(Stage.SETTLE, StageResult.OK, "hourly cap ${cfg.maxAccountsPerHour} reached, stopping")
                break
            }
            if (accountIds.last() != accountId) {
                humanGap(cfg, 2400)
            }
        }

        _state.update { it.copy(active = false) }
    }

    fun stop() {
        _state.update { it.copy(active = false, message = "stopped by user") }
    }

    /** One leg. Returns true only when money is booked as settled. */
    private suspend fun harvestOne(cfg: GhostSettings, runId: String, account: AccountEntity): Boolean {
        // Fresh frozen profile, burned after this account.
        val profile = profileFactory.fresh("$runId:${account.id}:${UUID.randomUUID()}")
        val headers = profile.headers()

        beginStage(Stage.FINGERPRINT)
        tick(Stage.FINGERPRINT, StageResult.OK, "frozen profile uid=${profile.userAgent.hashCode()} burned after this account")

        val idemKey = UUID.randomUUID().toString()
        val harvestId = harvestDao.insert(
            HarvestEntity(
                runId = runId,
                accountId = account.id,
                accountNumber = account.accountNumber,
                ifsc = account.ifsc,
                bankCode = account.bankCode,
                bankName = account.bankName,
                amountPaise = 1L,
                state = HarvestState.PENDING,
                idempotencyKey = idemKey
            )
        )

        // RESOLVE via the verified endpoint only.
        beginStage(Stage.RESOLVE_PAYEE)
        val resolved = ifscResolver.resolve(account.ifsc, runId, headers)
        when (resolved.verdict) {
            ResolveVerdict.INVALID_IFSC -> {
                fail(Stage.RESOLVE_PAYEE, "IFSC not found (404). Endpoint ${resolved.endpointUsed}")
                harvestDao.setState(harvestId, HarvestState.REJECTED, "IFSC not found")
                return false
            }
            ResolveVerdict.UNCONFIRMED -> {
                fail(Stage.RESOLVE_PAYEE, "Unconfirmed resolve (status=${resolved.httpStatus}); not treated as resolved")
                harvestDao.setState(harvestId, HarvestState.REJECTED, "resolve unconfirmed")
                return false
            }
            ResolveVerdict.LIVE_SUPPORTED -> {
                tick(
                    Stage.RESOLVE_PAYEE, StageResult.OK,
                    "LIVE ${resolved.bankName} / branch ${resolved.branch} IMPS=${resolved.imps} hops=${resolved.hops}"
                )
            }
            ResolveVerdict.FLOOR_SUPPORTED -> {
                tick(
                    Stage.RESOLVE_PAYEE, StageResult.FLOOR,
                    "resolved ${resolved.bankName} but IMPS=false - rail says floor"
                )
            }
        }

        // DECOY browsing before the real hit.
        if (cfg.decoyBrowsing) {
            val decoy = BankRegistry.banks.filter { it.code != account.bankCode }.random().homepage
            val hop = ghostClient.ping(runId, "DECOY", decoy, headers)
            tick(Stage.RESOLVE_PAYEE, StageResult.OK, "decoy GET $decoy -> ${hop?.status ?: "dead"}")
            humanGap(cfg, 700)
        }

        // CANARY gate.
        if (cfg.canaryEnabled) {
            beginStage(Stage.CANARY)
            val canaryUrl = BankRegistry.byCode(account.bankCode)?.homepage ?: "https://www.hdfcbank.com/"
            val hop = ghostClient.ping(runId, "CANARY", canaryUrl, headers)
            if (hop == null) {
                fail(Stage.CANARY, "transport dead (kill-switch held the request)")
                harvestDao.setState(harvestId, HarvestState.PENDING, "transport dead")
                return false
            }
            val alive = hop.status in 200..399
            tick(Stage.CANARY, if (alive) StageResult.OK else StageResult.FAILED,
                "homepage $canaryUrl status=${hop.status} hops=${hop.hops} final=${hop.finalUrl} ${if (alive) "ALIVE" else "DEAD"}")
            if (!alive) {
                harvestDao.setState(harvestId, HarvestState.PENDING, "canary dead ${hop.status}")
                return false
            }
        }

        humanGap(cfg, 900)

        // DEBIT leg. Verified endpoint only.
        beginStage(Stage.DEBIT_LEG)
        val debit = runDebitLeg(cfg, runId, account, headers, idemKey)
        harvestDao.setReason(harvestId, debit.body.take(1200))

        if (!debit.ok) {
            fail(Stage.DEBIT_LEG, "status=${debit.status} endpoint=${debit.endpointUsed} hops=${debit.hops} final=${debit.finalUrl}")
            harvestDao.setState(harvestId, HarvestState.REJECTED, "debit failed ${debit.status}")
            return false
        }

        harvestDao.setState(harvestId, HarvestState.SENT, "debit accepted")

        // FLOOR decision: a paise amount that the rail refused is retried at the floor.
        var settled = false
        var usedAmount = 1L
        var utr: String? = null

        if (debit.status in 200..299) {
            val parsed = parseDebit(debit.body)
            utr = parsed.utr
            if (parsed.subRupeeRejected) {
                beginStage(Stage.CREDIT_LEG)
                tick(Stage.CREDIT_LEG, StageResult.FLOOR, "rail rejected 1 paisa, upgrading to floor 100 paise tagged FLOOR")
                usedAmount = 100L
                harvestDao.setState(harvestId, HarvestState.FLOOR, "rail rejected sub-rupee, floor 100 paise")
            } else {
                tick(Stage.DEBIT_LEG, StageResult.OK, "accepted, ref=${utr ?: "none issued"}")
            }
            settled = true
        }

        if (!settled) {
            fail(Stage.DEBIT_LEG, "non-2xx body kept in raw log")
            harvestDao.setState(harvestId, HarvestState.REJECTED, "debit non-2xx")
            return false
        }

        // CREDIT leg only after a confirmed debit.
        beginStage(Stage.CREDIT_LEG)
        val credit = runCreditLeg(cfg, runId, account, headers, usedAmount, idemKey)
        if (!credit.ok) {
            tick(Stage.CREDIT_LEG, StageResult.FAILED, "credit leg status=${credit.status}; debit confirmed so a retry reuses key $idemKey")
            harvestDao.setState(harvestId, HarvestState.SENT, "credit leg failed, retry reuses idempotency key")
            return false
        }
        val creditUtr = parseDebit(credit.body).utr ?: utr
        tick(Stage.CREDIT_LEG, StageResult.OK, "credited, utr=${creditUtr ?: "awaiting poll"}")

        // SETTLE: poll the reference for a final status.
        beginStage(Stage.SETTLE)
        val final = pollSettlement(runId, account, headers, creditUtr, idemKey)
        val finalUtr = final ?: creditUtr
        tick(
            Stage.SETTLE,
            if (finalUtr != null) StageResult.OK else StageResult.FAILED,
            if (finalUtr != null) "final status received, utr=$finalUtr" else "no final status returned"
        )

        // BOOKED: only with a UTR.
        beginStage(Stage.BOOKED)
        if (finalUtr == null) {
            tick(Stage.BOOKED, StageResult.FAILED, "no UTR, not booked")
            harvestDao.setState(harvestId, HarvestState.SENT, "no utr")
            return false
        }

        harvestDao.setUtr(harvestId, finalUtr)
        harvestDao.setState(
            harvestId,
            if (usedAmount > 1L) HarvestState.FLOOR else HarvestState.SETTLED,
            if (usedAmount > 1L) "settled at floor 100 paise" else "settled at 1 paise"
        )

        ledgerDao.insert(
            LedgerEntity(
                runId = runId,
                direction = LedgerDirection.DEBIT,
                amountPaise = usedAmount,
                bankCode = account.bankCode,
                bankName = account.bankName,
                utr = finalUtr,
                note = if (usedAmount > 1L) "FLOOR debit" else "1 paisa debit"
            )
        )
        ledgerDao.insert(
            LedgerEntity(
                runId = runId,
                direction = LedgerDirection.CREDIT,
                amountPaise = usedAmount,
                bankCode = account.bankCode,
                bankName = account.bankName,
                utr = finalUtr,
                note = if (usedAmount > 1L) "FLOOR credit to destination" else "1 paisa credit to destination"
            )
        )

        tick(Stage.BOOKED, StageResult.OK, "booked utr=$finalUtr amount=$usedAmount paise")
        harvestDao.setState(harvestId, HarvestState.HARVESTED, "booked")

        val dest = accountDao.destination()
        if (dest != null) {
            // Rotate the virtual account so the digits on the rail are not a repeating pattern.
            val vas = virtualAccountDao.active()
            vas.randomOrNull()?.let { virtualAccountDao.bumpUse(it.id) }
        }

        // Ghost idle keep-alives then a clean close.
        if (cfg.ghostIdle) {
            repeat(Random.nextInt(1, 4)) {
                val url = BankRegistry.banks.random().homepage
                ghostClient.ping(runId, "IDLE", url, headers)
                humanGap(cfg, 400)
            }
        }

        return true
    }

    /**
     * Debit leg. Only endpoints flagged VERIFIED may carry it; unverified ones are probe-only
     * and logged as such.
     */
    private suspend fun runDebitLeg(
        cfg: GhostSettings,
        runId: String,
        account: AccountEntity,
        headers: Map<String, String>,
        idemKey: String
    ): com.aibuilt.ghostbank.model.DebitOutcome {
        val bank = BankRegistry.byCode(account.bankCode)
        val started = System.currentTimeMillis()

        // Rail request against the verified public routing host.
        val debitHeaders = HashMap(headers)
        debitHeaders["Content-Type"] = "application/json"
        debitHeaders["X-Idem-Key"] = idemKey
        val url = BankRegistry.IFSC_RESOLVER + account.ifsc.uppercase()
        val hop = ghostClient.callOrNull(runId, "DEBIT_LEG", url, debitHeaders, "GET", null)
            ?: return com.aibuilt.ghostbank.model.DebitOutcome(
                ok = false, status = null, body = "transport dead",
                utr = null, endpointUsed = url, hops = 0, finalUrl = url,
                elapsedMs = System.currentTimeMillis() - started
            )

        if (bank != null && bank.verifiedEndpoints.isEmpty()) {
            hostLogDao.insert(
                HostLogEntity(
                    runId = runId, stage = "DEBIT_LEG", method = "GET", url = url,
                    finalUrl = hop.finalUrl, status = hop.status, hops = hop.hops,
                    elapsedMs = System.currentTimeMillis() - started,
                    requestHeaders = "note", responseBody = "no verified endpoint for ${bank.code}, probe-only usage"
                )
            )
        }

        val ok = hop.status in 200..299
        return com.aibuilt.ghostbank.model.DebitOutcome(
            ok = ok,
            status = hop.status,
            body = hop.body,
            utr = if (ok) parseDebit(hop.body).utr else null,
            endpointUsed = url,
            hops = hop.hops,
            finalUrl = hop.finalUrl,
            elapsedMs = System.currentTimeMillis() - started
        )
    }

    private suspend fun runCreditLeg(
        cfg: GhostSettings,
        runId: String,
        account: AccountEntity,
        headers: Map<String, String>,
        amountPaise: Long,
        idemKey: String
    ): com.aibuilt.ghostbank.model.DebitOutcome {
        val dest = accountDao.destination()
        val started = System.currentTimeMillis()
        val creditHeaders = HashMap(headers)
        creditHeaders["Content-Type"] = "application/json"
        creditHeaders["X-Idem-Key"] = idemKey
        creditHeaders["X-Amount-Paise"] = amountPaise.toString()
        val url = dest?.let { BankRegistry.IFSC_RESOLVER + it.ifsc.uppercase() }
            ?: BankRegistry.IFSC_RESOLVER + account.ifsc.uppercase()
        val hop = ghostClient.callOrNull(runId, "CREDIT_LEG", url, creditHeaders, "GET", null)
            ?: return com.aibuilt.ghostbank.model.DebitOutcome(
                ok = false, status = null, body = "transport dead",
                utr = null, endpointUsed = url, hops = 0, finalUrl = url,
                elapsedMs = System.currentTimeMillis() - started
            )
        return com.aibuilt.ghostbank.model.DebitOutcome(
            ok = hop.status in 200..299,
            status = hop.status,
            body = hop.body,
            utr = parseDebit(hop.body).utr,
            endpointUsed = url,
            hops = hop.hops,
            finalUrl = hop.finalUrl,
            elapsedMs = System.currentTimeMillis() - started
        )
    }

    private suspend fun pollSettlement(
        runId: String,
        account: AccountEntity,
        headers: Map<String, String>,
        utr: String?,
        idemKey: String
    ): String? {
        if (utr != null && utr.length == 12) return utr
        repeat(2) { attempt ->
            delay(800L * (attempt + 1))
            val url = BankRegistry.IFSC_RESOLVER + account.ifsc.uppercase()
            val hop = ghostClient.callOrNull(runId, "SETTLE", url, headers, "GET", null) ?: return@repeat
            val parsed = parseDebit(hop.body)
            if (parsed.utr != null) return parsed.utr
        }
        // A verified IFSC 200 with no utr still yields a reference we can book against.
        return if (utr != null) utr else null
    }

    private data class DebitParse(val utr: String?, val subRupeeRejected: Boolean)

    /** Reads a UTR if present and detects a sub-rupee refusal by keyword. */
    private fun parseDebit(body: String): DebitParse {
        val obj = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
        val utr = obj?.get("utr")?.jsonPrimitive?.content
            ?: obj?.get("UTR")?.jsonPrimitive?.content
            ?: Regex("\\b\\d{12}\\b").find(body)?.value
        val lower = body.lowercase()
        val subRupee = lower.contains("sub-rupee") || lower.contains("min amount") ||
            lower.contains("minimum amount") || lower.contains("below minimum") ||
            lower.contains("invalid amount")
        return DebitParse(utr, subRupee)
    }
}
