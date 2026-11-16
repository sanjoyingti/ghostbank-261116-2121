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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StageTimeline
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.paiseLabel
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

@Composable
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    var amountText by rememberSaveable { mutableStateOf("1") }
    val amountPaise = amountText.toLongOrNull()?.coerceAtLeast(1L) ?: 1L

    Scaffold(topBar = { GhostTopBar(title = "Transfer", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Amount")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            amountText = input.filter { it.isDigit() }.take(9)
                        },
                        label = { Text("Amount in paise (1 = one paisa)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Ledger keeps exact paise: ${paiseLabel(amountPaise)} = $amountPaise paise. " +
                            "A rail refusal under Rs 1 flips the leg to 100 paise and tags it FLOOR.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Source accounts")
                    StatusPill("${ui.selected.size} selected")
                }
            }

            if (ui.accounts.isEmpty()) {
                item {
                    GhostCard {
                        Text(
                            "No accounts saved yet. Add one on the Accounts tab first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(ui.accounts) { account ->
                    val selected = ui.selected.contains(account.id)
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle(maskAccount(account.accountNumber))
                            StatusPill(if (selected) "queued" else account.bankCode)
                        }
                        KeyValue("IFSC", account.ifsc)
                        KeyValue("Bank", account.bankName)
                        OutlinedButton(onClick = { vm.toggle(account.id) }) {
                            Text(if (selected) "Remove from run" else "Add to run")
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { vm.startWith(ui.selected.toList(), amountPaise) },
                        enabled = !run.active && ui.selected.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Start pipeline") }
                    OutlinedButton(
                        onClick = vm::stop,
                        enabled = run.active,
                        modifier = Modifier.weight(1f)
                    ) { Text("Stop") }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Pipeline")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(
                            if (run.active) "running" else "idle",
                            if (run.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        StatusPill("${run.completedLegs}/${run.totalLegs} legs")
                        StatusPill("${run.failedLegs} failed", MaterialTheme.colorScheme.error)
                    }
                    Text(run.message, style = MaterialTheme.typography.bodyMedium)
                    StageTimeline(ticks = run.ticks)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth score")
                    KeyValue("Score", "${run.stealthScore}/100")
                    if (run.stealthReasons.isNotEmpty()) {
                        MonoBlock(run.stealthReasons.joinToString("\n"), maxHeight = 160)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Raw log")
                    Text(
                        "Full response bodies stay here, so a failed leg needs no screenshot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MonoBlock(
                        if (run.rawTail.isEmpty()) "no traffic yet" else run.rawTail.joinToString("\n"),
                        maxHeight = 280
                    )
                }
            }
        }
    }
}
