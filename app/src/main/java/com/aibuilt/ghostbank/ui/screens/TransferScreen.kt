package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StageTimeline
import com.aibuilt.ghostbank.ui.components.StatusPill
import kotlinx.coroutines.launch

@Composable
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val accounts by remember { container.accountDao.observeAll() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val run by container.harvestEngine.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedId by remember { mutableStateOf<Long?>(null) }
    var amountText by remember { mutableStateOf("1.00") }
    var error by remember { mutableStateOf<String?>(null) }

    fun start() {
        val id = selectedId ?: run { error = "Pick an account first"; return }
        val paise = parseRupeesToPaise(amountText)
        if (paise == null || paise <= 0L) {
            error = "Amount must be a positive rupee value like 1 or 12.50"
            return
        }
        val account = accounts.firstOrNull { it.id == id }
        val cap = BankRegistry.byCode(account?.bankCode ?: "")?.perTxCapPaise
        if (cap != null && paise > cap) {
            error = "Above this bank's per-transfer cap of ${cap / 100} rupees"
            return
        }
        error = null
        scope.launch { container.harvestEngine.harvest(listOf(id), paise) }
    }

    Scaffold(topBar = { GhostTopBar(title = "Transfer", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Amount")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.filter { c -> c.isDigit() || c == '.' }.take(12)
                        },
                        label = { Text("Rupees (kept exactly in paise)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item { SectionTitle("Source account") }

            if (accounts.isEmpty()) {
                item {
                    Text(
                        "No accounts yet. Add one on the Account tab first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(accounts, key = { it.id }) { account ->
                val picked = account.id == selectedId
                GhostCard(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.holderLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${account.bankName} - ${account.ifsc}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (picked) StatusPill("selected", MaterialTheme.colorScheme.primary)
                    }
                    OutlinedButton(onClick = { selectedId = account.id }) { Text("Use this") }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { start() },
                        enabled = !run.active,
                        modifier = Modifier.weight(1f)
                    ) { Text("Run pipeline") }
                    OutlinedButton(
                        onClick = { container.harvestEngine.stop() },
                        enabled = run.active,
                        modifier = Modifier.weight(1f)
                    ) { Text("Stop") }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Pipeline")
                    Text(
                        run.message.ifBlank { "Idle. Stages tick only when the far end confirms." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StageTimeline(run.ticks)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Raw log")
                    MonoBlock(
                        text = if (run.rawTail.isEmpty()) "nothing yet" else run.rawTail.joinToString("\n"),
                        maxHeight = 260
                    )
                }
            }
        }
    }
}

/** "12.5" -> 1250 paise. Null when it cannot be parsed. */
internal fun parseRupeesToPaise(text: String): Long? {
    if (text.isBlank()) return null
    val parts = text.split(".")
    if (parts.size > 2) return null
    val rupees = parts[0].ifBlank { "0" }.toLongOrNull() ?: return null
    val paisePart = parts.getOrNull(1).orEmpty()
    if (paisePart.length > 2) return null
    val paise = paisePart.padEnd(2, '0').ifBlank { "0" }.toLongOrNull() ?: return null
    return rupees * 100L + paise
}
