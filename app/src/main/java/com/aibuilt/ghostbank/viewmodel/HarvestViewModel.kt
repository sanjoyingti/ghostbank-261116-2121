package com.aibuilt.ghostbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.AccountEntity
import com.aibuilt.ghostbank.data.db.HarvestEntity
import com.aibuilt.ghostbank.engine.RunState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HarvestUi(
    val accounts: List<AccountEntity> = emptyList(),
    val harvests: List<HarvestEntity> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val toast: String? = null
)

class HarvestViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(HarvestUi())
    val ui: StateFlow<HarvestUi> = _ui.asStateFlow()

    val run: StateFlow<RunState> = container.harvestEngine.state

    init {
        viewModelScope.launch {
            container.accountDao.observeAll().collect { accounts ->
                _ui.value = _ui.value.copy(accounts = accounts)
            }
        }
        viewModelScope.launch {
            container.harvestDao.observeRecent().collect { list ->
                _ui.value = _ui.value.copy(harvests = list)
            }
        }
    }

    fun toggle(id: Long) {
        val set = _ui.value.selected.toMutableSet()
        if (!set.add(id)) set.remove(id)
        _ui.value = _ui.value.copy(selected = set)
    }

    fun selectAllUnharvested() {
        val harvested = _ui.value.harvests
            .filter { it.state.name in setOf("SETTLED", "FLOOR", "HARVESTED") }
            .map { it.accountId }
            .toSet()
        _ui.value = _ui.value.copy(
            selected = _ui.value.accounts.filter { it.id !in harvested }.map { it.id }.toSet()
        )
    }

    fun clearSelection() {
        _ui.value = _ui.value.copy(selected = emptySet())
    }

    fun start() {
        val ids = _ui.value.selected.toList()
        if (ids.isEmpty()) {
            _ui.value = _ui.value.copy(toast = "Select at least one account")
            return
        }
        viewModelScope.launch {
            container.harvestEngine.harvest(ids)
        }
    }

    fun startWith(ids: List<Long>, amountPaise: Long) {
        if (ids.isEmpty()) {
            _ui.value = _ui.value.copy(toast = "Pick a source account first")
            return
        }
        viewModelScope.launch {
            container.harvestEngine.harvest(ids, amountPaise.coerceAtLeast(1L))
        }
    }

    fun stop() = container.harvestEngine.stop()

    fun clearToast() {
        _ui.value = _ui.value.copy(toast = null)
    }

    fun resetHistory() {
        viewModelScope.launch { container.harvestDao.clearAll() }
    }

    fun stateOf(accountId: Long): String? =
        _ui.value.harvests.firstOrNull { it.accountId == accountId }?.state?.name

    val totals: StateFlow<Long> = container.harvestDao.observeTotalPaise()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
}
