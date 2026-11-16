package com.aibuilt.ghostbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.HostLogEntity
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.data.stealth.GhostProfileFactory
import com.aibuilt.ghostbank.model.LeakFinding
import com.aibuilt.ghostbank.model.StealthLevel
import com.aibuilt.ghostbank.model.StealthReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LeakUi(
    val running: Boolean = false,
    val findings: List<LeakFinding> = emptyList(),
    val report: StealthReport? = null
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<GhostSettings> = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GhostSettings())

    private val _leak = MutableStateFlow(LeakUi())
    val leak: StateFlow<LeakUi> = _leak.asStateFlow()

    private val _proxyText = MutableStateFlow<String?>(null)
    val proxyText: StateFlow<String?> = _proxyText.asStateFlow()

    val hostLog: StateFlow<List<HostLogEntity>> = container.hostLogDao.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val levels = StealthLevel.entries

    fun loadProxies(text: String) {
        _proxyText.value = text
    }

    fun saveProxies(text: String) {
        viewModelScope.launch {
            container.settingsStore.setProxies(
                text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            )
            _proxyText.value = null
        }
    }

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

    fun score() {
        viewModelScope.launch {
            val cfg = container.settingsStore.snapshot()
            val report = container.traceAuditor.stealthScore(cfg, canaryAlive = cfg.exitConfigured, hopsSeen = 0)
            _leak.value = _leak.value.copy(report = report)
        }
    }

    fun clearLogs() = viewModelScope.launch { container.hostLogDao.clearAll() }
}
