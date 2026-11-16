package com.aibuilt.ghostbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.data.db.AccountEntity
import com.aibuilt.ghostbank.engine.RunState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransferUi(
    val accounts: List<AccountEntity> = emptyList(),
    val amountPaiseText: String = "1",
    val selected: Set<Long> = emptySet(),
    val error: String? = null
)

class TransferViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(TransferUi())
    val ui: StateFlow<TransferUi> = _ui.asStateFlow()

    val run: StateFlow<RunState> = container.harvestEngine.state

    init {
        viewModelScope.launch {
            container.accountDao.observeAll().collect { list ->
                _ui.value = _ui.value.copy(accounts = list)
            }
        }
    }

    fun setAmount(text: String) {
        val clean = text.filter { it.isDigit() }.take(9)
        _ui.value = _ui.value.copy(amountPaiseText = clean, error = null)
    }

    fun resetToPaisa() {
        _ui.value = _ui.value.copy(amountPaiseText = "1", error = null)
    }

    fun toggle(id: Long) {
        val set = _ui.value.selected.toMutableSet()
        if (!set.add(id)) set.remove(id)
        _ui.value = _ui.value.copy(selected = set, error = null)
    }

    fun selectAll() {
        _ui.value = _ui.value.copy(selected = _ui.value.accounts.map { it.id }.toSet())
    }

    fun clearSelection() {
        _ui.value = _ui.value.copy(selected = emptySet())
    }

    fun start() {
        val amount = _ui.value.amountPaiseText.toLongOrNull()
        if (amount == null || amount < 1L) {
            _ui.value = _ui.value.copy(error = "Amount in paise must be at least 1")
            return
        }
        val ids = _ui.value.selected.toList()
        if (ids.isEmpty()) {
            _ui.value = _ui.value.copy(error = "Select at least one source account")
            return
        }
        val caps = ids.mapNotNull { id -> _ui.value.accounts.firstOrNull { it.id == id } }
            .mapNotNull { BankRegistry.byCode(it.bankCode)?.perTxCapPaise }
        if (caps.isNotEmpty() && amount > caps.min()) {
            _ui.value = _ui.value.copy(
                error = "Hard cap of the strictest selected bank is ${caps.min()} paise"
            )
            return
        }
        viewModelScope.launch { container.harvestEngine.harvest(ids, amount) }
    }

    fun stop() = container.harvestEngine.stop()

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }
}
