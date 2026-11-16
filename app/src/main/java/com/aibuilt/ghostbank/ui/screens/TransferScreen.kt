package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

@Composable
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()

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
                    SectionTitle("Destination (credit lands here)")
                    if (destination == null) {
                        Text(
                            "No destination set. Open Accounts, save one account as destination first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        KeyValue("Account", maskAccount(destination!!.accountNumber))
                        KeyValue("IFSC", destination!!.ifsc)
                        KeyValue("Bank", destination!!.bankName)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount (paise, exactly)")
                    OutlinedTextField(
                        value = ui.amountPaise.toString(),
                        onValueChange = { text ->
                            val digits = text.filter { it.isDigit() }.take(8)
                            vm.setAmountPaise(digits.toLongOrNull() ?: 1L)
                        },
                        label = { Text("Paise - 1 means one paisa") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1L, 100L, 500L, 1000L).forEach { value ->
                            FilterChip(
                                selected = ui.amountPaise == value,
                                onClick = { vm.setAmountPaise(value) },
                                label = { Text(if (value == 1L) "1 paisa" else "$value paise") }
                            )
                        }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Source account")
                    if (ui.accounts.isEmpty()) {
                        Text("No accounts saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ui.accounts.forEach { account ->
                        FilterChip(
                            selected = ui.sourceId == account.id ||
                                (ui.sourceId == null && ui.accounts.firstOrNull()?.id == account.id),
                            onClick = { vm.setSource(account.id) },
                            label = { Text("${account.bankName} ${maskAccount(account.accountNumber)}") },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = vm::startSingle,
                        enabled = !run.active,
                        modifier = Modifier.weight(1f)
                    ) { Text("Run the pipeline") }
                    OutlinedButton(
                        onClick = vm::stop,
                        enabled = run.active,
                        modifier = Modifier.weight(1f)
                    ) { Text("Stop") }
                }
            }

            if (run.active) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            item {
                GhostCard {
                    SectionTitle("Stages (timings in ms)")
                    StageTimeline(ticks = run.ticks)
                }
            }

            if (run.stealthReasons.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Stealth score ${run.stealthScore}/100")
                        run.stealthReasons.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (run.rawTail.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Raw log")
                        MonoBlock(text = run.rawTail.joinToString("\n"))
                    }
                }
            }

            item {
                Text(
                    "A debit that answers 404 or any non-2xx stops the run as FAILED, keeps the body in the raw log, and the credit leg never starts. A retry reuses the same idempotency key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
