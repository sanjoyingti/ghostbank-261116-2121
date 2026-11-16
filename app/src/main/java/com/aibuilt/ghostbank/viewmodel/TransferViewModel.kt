package com.aibuilt.ghostbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.data.db.AccountEntity
import com.aibuilt.ghostbank.engine.RunState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransferUi(
    val amountInput: String = "0.01",
    val amountPaise: Long = 1L,
    val sourceId: Long? = null,
    val error: String? = null
)

class TransferViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(TransferUi())
    val ui: StateFlow<TransferUi> = _ui.asStateFlow()

    val run: StateFlow<RunState> = container.harvestEngine.state

    val accounts: StateFlow<List<AccountEntity>> = container.accountDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val destination: StateFlow<AccountEntity?> = container.accountDao.observeDestination()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Amount is never held as a double: rupees are parsed straight into integer paise. */
    fun setAmount(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        val pieces = filtered.split(".")
        val rupees = pieces.firstOrNull().orEmpty().take(7).toLongOrNull() ?: 0L
        val paise = pieces.getOrNull(1).orEmpty().take(2).padEnd(2, '0').toLongOrNull() ?: 0L
        var total = rupees * 100L + paise
        if (total < 1L) total = 1L
        _ui.value = _ui.value.copy(amountInput = filtered, amountPaise = total, error = null)
    }

    fun setOnePaisa() {
        _ui.value = _ui.value.copy(amountInput = "0.01", amountPaise = 1L, error = null)
    }

    fun selectSource(id: Long) {
        _ui.value = _ui.value.copy(sourceId = id, error = null)
    }

    fun start() {
        val sourceId = _ui.value.sourceId
        if (sourceId == null) {
            _ui.value = _ui.value.copy(error = "Pick a source account first")
            return
        }
        val account = accounts.value.firstOrNull { it.id == sourceId }
        if (account == null) {
            _ui.value = _ui.value.copy(error = "That account is gone from the registry")
            return
        }
        val cap = BankRegistry.byCode(account.bankCode)?.perTxCapPaise ?: 2_500_000L
        if (_ui.value.amountPaise > cap) {
            _ui.value = _ui.value.copy(error = "Above the ${account.bankName} per-transaction cap")
            return
        }
        if (destination.value == null) {
            _ui.value = _ui.value.copy(error = "Mark a destination account before running")
            return
        }
        _ui.value = _ui.value.copy(error = null)
        val amount = _ui.value.amountPaise
        viewModelScope.launch { container.harvestEngine.harvest(listOf(sourceId), amount) }
    }

    fun stop() = container.harvestEngine.stop()
}
