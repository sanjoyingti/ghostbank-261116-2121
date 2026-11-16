package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import kotlin.math.roundToLong

/** Rupee text to exact paise, no floating drift. */
fun rupeesToPaise(text: String): Long? {
    val value = text.trim().toDoubleOrNull() ?: return null
    if (value < 0) return null
    return (value * 100.0).roundToLong().coerceAtLeast(1L)
}

@Composable
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()

    var amountText by remember { mutableStateOf("0.01") }

    Scaffold(topBar = { GhostTopBar(title = "Transfer", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Destination")
                    if (destination == null) {
                        Text(
                            "No destination marked. Save an account as destination first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        KeyValue("Account", maskAccount(destination.accountNumber))
                        KeyValue("IFSC", destination.ifsc)
                        KeyValue("Bank", destination.bankName)
                        StatusPill("credit leg target", MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Source account")
                    if (ui.accounts.isEmpty()) {
                        Text(
                            "No source accounts yet. Go back and add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ui.accounts, key = { it.id }) { account ->
                                FilterChip(
                                    selected = ui.sourceId == account.id,
                                    onClick = { vm.setSource(account.id) },
                                    label = { Text(maskAccount(account.accountNumber)) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Rupees") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val paise = rupeesToPaise(amountText)
                    Text(
                        if (paise == null) "Amount not readable yet"
                        else "${paiseLabel(paise)} = $paise paise (kept as an exact integer)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { amountText = "0.01" }) { Text("0.01 (1 paisa)") }
                        OutlinedButton(onClick = { amountText = "1" }) { Text("1 (floor)") }
                        OutlinedButton(onClick = { amountText = "5" }) { Text("5") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                val value = rupeesToPaise(amountText) ?: 1L
                                vm.setAmountPaise(value)
                                vm.startSingle()
                            },
                            enabled = !run.active && ui.accounts.isNotEmpty()
                        ) { Text("Run pipeline") }
                        OutlinedButton(onClick = vm::stop, enabled = run.active) {
                            androidx.compose.material3.Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Stop")
                        }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Pipeline")
                    Text(
                        "Debit must confirm before the credit leg starts. A failed leg keeps its whole response body below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    KeyValue("Run", run.runId.ifBlank { "-" })
                    KeyValue("Current stage", run.current.name)
                    KeyValue("Legs", "${run.completedLegs}/${run.totalLegs} failed=${run.failedLegs}")
                    StageTimeline(run.ticks)
                }
            }

            item { SectionTitle("Raw log") }
            item { MonoBlock(run.rawTail.joinToString("\n").ifBlank { "nothing yet" }, maxHeight = 300) }
        }
    }
}
