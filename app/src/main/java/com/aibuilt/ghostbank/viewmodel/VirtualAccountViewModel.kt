package com.aibuilt.ghostbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.VirtualAccountEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VirtualAccountViewModel(private val container: AppContainer) : ViewModel() {

    val accounts: StateFlow<List<VirtualAccountEntity>> = container.virtualAccountDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(number: String, ifsc: String, label: String) {
        if (number.length < 9 || ifsc.length != 11) return
        viewModelScope.launch {
            container.virtualAccountDao.insert(
                VirtualAccountEntity(
                    accountNumber = number,
                    ifsc = ifsc,
                    bankCode = ifsc.take(4),
                    label = label,
                    active = true
                )
            )
        }
    }

    fun toggle(id: Long, active: Boolean) {
        viewModelScope.launch { container.virtualAccountDao.setActive(id, active) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { container.virtualAccountDao.delete(id) }
    }
}
