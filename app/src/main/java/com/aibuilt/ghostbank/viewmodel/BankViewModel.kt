package com.aibuilt.ghostbank.viewmodel

import androidx.lifecycle.ViewModel
import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.model.Bank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BankViewModel : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val banks: List<Bank> = BankRegistry.banks

    fun setQuery(q: String) {
        _query.value = q
    }

    fun filtered(q: String): List<Bank> =
        if (q.isBlank()) banks
        else banks.filter {
            it.name.contains(q, true) || it.code.contains(q, true)
        }
}
