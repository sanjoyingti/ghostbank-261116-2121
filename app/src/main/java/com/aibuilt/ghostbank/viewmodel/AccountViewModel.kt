package com.aibuilt.ghostbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.data.db.AccountEntity
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.model.ResolveVerdict
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class AccountUi(
    val accountNumber: String = "",
    val ifsc: String = "",
    val label: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

class AccountViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(AccountUi())
    val ui: StateFlow<AccountUi> = _ui.asStateFlow()

    val accounts: StateFlow<List<AccountEntity>> = container.accountDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val destination: StateFlow<AccountEntity?> = container.accountDao.observeDestination()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<GhostSettings> = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GhostSettings())

    fun setAccount(v: String) {
        _ui.value = _ui.value.copy(accountNumber = v.filter { it.isDigit() }.take(18), error = null)
    }

    fun setIfsc(v: String) {
        _ui.value = _ui.value.copy(
            ifsc = v.uppercase().filter { it.isLetterOrDigit() }.take(11),
            error = null
        )
    }

    fun setLabel(v: String) {
        _ui.value = _ui.value.copy(label = v.take(40))
    }

    fun clearMessages() {
        _ui.value = _ui.value.copy(error = null, info = null)
    }

    private fun validate(): String? {
        val n = _ui.value.accountNumber
        if (n.length < 9) return "Account number must be at least 9 digits"
        val ifsc = _ui.value.ifsc
        if (!Regex("^[A-Z]{4}0[A-Z0-9]{6}$").matches(ifsc)) return "IFSC must look like SBIN0001722"
        if (BankRegistry.byIfscPrefix(ifsc) == null) return "Bank prefix ${ifsc.take(4)} is not in the registry"
        return null
    }

    fun save(makeDestination: Boolean) {
        validate()?.let {
            _ui.value = _ui.value.copy(error = it)
            return
        }
        _ui.value = _ui.value.copy(busy = true, error = null, info = null)
        viewModelScope.launch {
            val ifsc = _ui.value.ifsc
            val bank = BankRegistry.byIfscPrefix(ifsc)
            if (bank == null) {
                _ui.value = _ui.value.copy(busy = false, error = "Unknown bank")
                return@launch
            }
            // Resolve through the verified public IFSC endpoint before saving.
            val profile = container.profileFactory.fresh("save:$ifsc")
            val resolved = container.ifscResolver.resolve(ifsc, "acc-save", profile.headers())
            when (resolved.verdict) {
                ResolveVerdict.INVALID_IFSC -> {
                    _ui.value = _ui.value.copy(busy = false, error = "IFSC not found on the live resolver")
                    return@launch
                }
                ResolveVerdict.UNCONFIRMED -> {
                    _ui.value = _ui.value.copy(
                        busy = false,
                        error = "Resolver unconfirmed (status=${resolved.httpStatus}) - check the exit and try again"
                    )
                    return@launch
                }
                else -> Unit
            }

            val number = _ui.value.accountNumber
            val fp = fingerprint(number, ifsc)
            if (container.accountDao.byFingerprint(fp) != null) {
                _ui.value = _ui.value.copy(busy = false, error = "That account is already in the registry")
                return@launch
            }
            val id = container.accountDao.insert(
                AccountEntity(
                    accountNumber = number,
                    ifsc = ifsc,
                    bankCode = bank.code,
                    bankName = resolved.bankName ?: bank.name,
                    holderLabel = _ui.value.label.ifBlank { "Acct ending ${number.takeLast(4)}" },
                    fingerprint = fp,
                    isDestination = false
                )
            )
            if (makeDestination && id > 0) {
                container.accountDao.clearDestination()
                container.accountDao.setDestination(id)
                container.settingsStore.setDestination(number, ifsc)
            }
            val rail = when (resolved.verdict) {
                ResolveVerdict.LIVE_SUPPORTED -> "IMPS supported"
                ResolveVerdict.FLOOR_SUPPORTED -> "rail says floor, IMPS=false"
                else -> "resolved"
            }
            _ui.value = AccountUi(
                busy = false,
                info = "Added ${resolved.bankName ?: bank.name} - branch ${resolved.branch ?: "unknown"} ($rail)"
            )
        }
    }

    fun setDestination(id: Long) {
        viewModelScope.launch {
            container.accountDao.clearDestination()
            container.accountDao.setDestination(id)
            accounts.value.firstOrNull { it.id == id }?.let {
                container.settingsStore.setDestination(it.accountNumber, it.ifsc)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { container.accountDao.delete(id) }
    }

    private fun fingerprint(number: String, ifsc: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("$number|$ifsc".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
