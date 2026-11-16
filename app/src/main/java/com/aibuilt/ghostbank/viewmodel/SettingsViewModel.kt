package com.aibuilt.ghostbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.data.db.HostLogEntity
import com.aibuilt.ghostbank.data.db.VirtualAccountEntity
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.data.stealth.GhostProfileFactory
import com.aibuilt.ghostbank.model.CanaryOutcome
import com.aibuilt.ghostbank.model.LeakFinding
import com.aibuilt.ghostbank.model.StealthLevel
import com.aibuilt.ghostbank.model.StealthReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LeakUi(
    val running: Boolean = false,
    val findings: List<LeakFinding> = emptyList(),
    val report: StealthReport? = null
)

data class CanaryUi(
    val running: Boolean = false,
    val outcome: CanaryOutcome? = null
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<GhostSettings> = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GhostSettings())

    private val _leak = MutableStateFlow(LeakUi())
    val leak: StateFlow<LeakUi> = _leak.asStateFlow()

    private val _canary = MutableStateFlow(CanaryUi())
    val canary: StateFlow<CanaryUi> = _canary.asStateFlow()

    val hostLog: StateFlow<List<HostLogEntity>> = container.hostLogDao.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val virtualAccounts: StateFlow<List<VirtualAccountEntity>> = container.virtualAccountDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val levels = StealthLevel.entries

    fun setLevel(level: StealthLevel) = viewModelScope.launch { container.settingsStore.setStealthLevel(level) }
    fun setTor(v: Boolean) = viewModelScope.launch { container.settingsStore.setTorMode(v) }
    fun setDoh(v: Boolean) = viewModelScope.launch { container.settingsStore.setDoh(v) }
    fun setKillSwitch(v: Boolean) = viewModelScope.launch { container.settingsStore.setKillSwitch(v) }
    fun setDiurnal(v: Boolean) = viewModelScope.launch { container.settingsStore.setDiurnal(v) }
    fun setWindow(start: Int, end: Int) = viewModelScope.launch { container.settingsStore.setWindow(start, end) }
    fun setCaps(rpm: Int, aph: Int) = viewModelScope.launch { container.settingsStore.setCaps(rpm, aph) }
    fun setDecoy(v: Boolean) = viewModelScope.launch { container.settingsStore.setDecoy(v) }
    fun setGhostIdle(v: Boolean) = viewModelScope.launch { container.settingsStore.setGhostIdle(v) }
    fun setFragmentation(v: Boolean) = viewModelScope.launch { container.settingsStore.setFragmentation(v) }
    fun setCanary(v: Boolean) = viewModelScope.launch { container.settingsStore.setCanary(v) }
    fun setPadding(v: Boolean) = viewModelScope.launch { container.settingsStore.setPadding(v) }
    fun setGeo(v: Boolean) = viewModelScope.launch { container.settingsStore.setGeoMatch(v) }
    fun setExactPaisa(v: Boolean) = viewModelScope.launch { container.settingsStore.setExactOnePaisa(v) }

    fun saveProxies(lines: List<String>) {
        viewModelScope.launch { container.settingsStore.setProxies(lines) }
    }

    fun runLeakCheck() {
        if (_leak.value.running) return
        _leak.value = LeakUi(running = true)
        viewModelScope.launch {
            val cfg = container.settingsStore.snapshot()
            val profile = GhostProfileFactory().fresh("leak:${System.currentTimeMillis()}")
            val findings = container.traceAuditor.leakCheck(profile.headers())
            val report = container.traceAuditor.stealthScore(cfg, canaryAlive = true, hopsSeen = findings.size)
            _leak.value = LeakUi(running = false, findings = findings, report = report)
        }
    }

    fun runCanary() {
        if (_canary.value.running) return
        _canary.value = CanaryUi(running = true)
        viewModelScope.launch {
            val profile = GhostProfileFactory().fresh("canary:${System.currentTimeMillis()}")
            val outcome = container.traceAuditor.canary("canary-ui", profile.headers(), true)
            _canary.value = CanaryUi(running = false, outcome = outcome)
        }
    }

    fun score() {
        viewModelScope.launch {
            val cfg = container.settingsStore.snapshot()
            val report = container.traceAuditor.stealthScore(cfg, canaryAlive = cfg.exitConfigured, hopsSeen = 0)
            _leak.value = _leak.value.copy(report = report)
        }
    }

    fun clearLogs() = viewModelScope.launch { container.hostLogDao.clearAll() }

    fun addVirtualAccount(number: String, ifsc: String, label: String) {
        val cleanIfsc = ifsc.trim().uppercase()
        val bank = BankRegistry.byIfscPrefix(cleanIfsc)
        val valid = number.length >= 9 && Regex("^[A-Z]{4}0[A-Z0-9]{6}$").matches(cleanIfsc) && bank != null
        if (!valid) return
        viewModelScope.launch {
            container.virtualAccountDao.insert(
                VirtualAccountEntity(
                    accountNumber = number,
                    ifsc = cleanIfsc,
                    bankCode = bank.code,
                    label = label.ifBlank { "VA ending ${number.takeLast(4)}" }
                )
            )
        }
    }

    fun removeVirtualAccount(id: Long) {
        viewModelScope.launch { container.virtualAccountDao.delete(id) }
    }

    fun toggleVirtualAccount(id: Long, active: Boolean) {
        viewModelScope.launch { container.virtualAccountDao.setActive(id, active) }
    }
}
