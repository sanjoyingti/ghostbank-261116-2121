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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.data.db.AccountEntity
import com.aibuilt.ghostbank.engine.RunState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StageTimeline
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.paiseLabel
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

/** "1" -> 100 paise, "0.01" -> 1 paisa. Never returns less than one paisa. */
fun paiseOf(text: String): Long {
    val clean = text.trim().replace(",", "")
    if (clean.isEmpty()) return 1L
    val parts = clean.split('.')
    val rupees = parts.getOrNull(0)?.filter { it.isDigit() }?.toLongOrNull() ?: 0L
    val fraction = parts.getOrNull(1)?.filter { it.isDigit() }?.padEnd(2, '0')?.take(2)?.toLongOrNull() ?: 0L
    return (rupees * 100L + fraction).coerceAtLeast(1L)
}

@Composable
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    var amount by rememberSaveable { mutableStateOf("0.01") }

    TransferScreen(
        accounts = ui.accounts,
        selected = ui.selected,
        run = run,
        amountText = amount,
        onAmount = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
        onToggle = vm::toggle,
        onStart = { vm.startWith(ui.selected.toList(), paiseOf(amount)) },
        onStop = vm::stop,
        onBack = onBack
    )
}

@Composable
fun TransferScreen(
    accounts: List<AccountEntity>,
    selected: Set<Long>,
    run: RunState,
    amountText: String,
    onAmount: (String) -> Unit,
    onToggle: (Long) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit
) {
    val chosenBank = accounts.firstOrNull { it.id in selected }?.bankCode?.let { BankRegistry.byCode(it) }

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
                        onValueChange = onAmount,
                        label = { Text("Rupees (0.01 = 1 paisa)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("0.01", "1", "5").forEach { preset ->
                            FilterChip(
                                selected = amountText == preset,
                                onClick = { onAmount(preset) },
                                label = { Text(preset) }
                            )
                        }
                    }
                    Text(
                        "That is ${paiseOf(amountText)} paise exactly. " +
                            (chosenBank?.let { "Cap at ${chosenBank.name}: ${paiseLabel(it.perTxCapPaise)}." }
                                ?: "Pick a source account."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { SectionTitle("Source accounts") }

            if (accounts.isEmpty()) {
                item {
                    GhostCard {
                        Text(
                            "No accounts saved yet. Add one on the Account screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    maskAccount(account.accountNumber),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "${account.bankName} · ${account.ifsc}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilterChip(
                                selected = account.id in selected,
                                onClick = { onToggle(account.id) },
                                label = { Text(if (account.id in selected) "picked" else "pick") }
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStart,
                        enabled = !run.active && selected.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Run pipeline") }
                    OutlinedButton(
                        onClick = onStop,
                        enabled = run.active,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Stop,
                            contentDescription = null
                        )
                        Text("  Stop")
                    }
                }
            }

            item {
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Pipeline", modifier = Modifier.weight(1f))
                        StatusPill(
                            text = if (run.active) "running" else "idle",
                            color = if (run.active) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                    Text(
                        run.message.ifBlank { "Debit must confirm before the credit leg starts." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StageTimeline(run.ticks)
                }
            }

            if (run.stealthScore > 0) {
                item {
                    GhostCard {
                        SectionTitle("Stealth score ${run.stealthScore}/100")
                        run.stealthReasons.forEach {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Raw log (full body kept on failure)")
                    MonoBlock(
                        if (run.rawTail.isEmpty()) "no traffic yet" else run.rawTail.joinToString("\n")
                    )
                }
            }
        }
    }
}
