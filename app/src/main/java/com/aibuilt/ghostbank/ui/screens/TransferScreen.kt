package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.engine.RunState
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StageTimeline
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.paiseLabel
import com.aibuilt.ghostbank.ui.ghostFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import com.aibuilt.ghostbank.viewmodel.TransferUi
import com.aibuilt.ghostbank.viewmodel.TransferViewModel

@Composable
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: TransferViewModel = viewModel(factory = ghostFactory { TransferViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(GhostSettings())

    TransferScreen(
        ui = ui,
        run = run,
        settings = settings,
        onAmount = vm::setAmount,
        onToggle = vm::toggle,
        onSelectAll = vm::selectAll,
        onClear = vm::clearSelection,
        onStart = vm::start,
        onStop = vm::stop,
        onBack = onBack
    )
}

@Composable
fun TransferScreen(
    ui: TransferUi,
    run: RunState,
    settings: GhostSettings,
    onAmount: (String) -> Unit,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit
) {
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
                    SectionTitle("Leg")
                    KeyValue("Rail", "IMPS P2A")
                    KeyValue("Destination", settings.destinationAccount.ifBlank { "not set" })
                    KeyValue("Destination IFSC", settings.destinationIfsc.ifBlank { "not set" })
                    if (settings.destinationAccount.isBlank()) {
                        Text(
                            "No destination saved. Mark one on the Account screen or credits fall back to the source IFSC.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount in paise")
                    OutlinedTextField(
                        value = ui.amountPaiseText,
                        onValueChange = onAmount,
                        label = { Text("Paise (1 = one paisa)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "= ${paiseLabel(ui.amountPaiseText.toLongOrNull() ?: 0L)}. Sub-rupee is sent exactly; if the rail refuses it the leg floors to 100 paise and is tagged FLOOR.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onAmount.let { { onAmount("1") } }) { Text("Reset to 1 paisa") }
                    ui.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Sources (${ui.selected.size})")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onSelectAll) { Text("All") }
                    TextButton(onClick = onClear) { Text("None") }
                }
            }

            if (ui.accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.SwapVert,
                        title = "No source accounts",
                        subtitle = "Add accounts first, then come back to move money."
                    )
                }
            }

            items(ui.accounts, key = { it.id }) { account ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = ui.selected.contains(account.id),
                            onCheckedChange = { onToggle(account.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.holderLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${account.bankName} - ${account.ifsc}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                if (run.active) {
                    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                        Text("Stop run")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        enabled = ui.selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Run pipeline")
                    }
                }
            }

            item {
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Pipeline")
                        Spacer(Modifier.width(8.dp))
                        StatusPill(
                            if (run.active) "running" else "idle",
                            if (run.active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                        )
                    }
                    StageTimeline(run.ticks)
                    if (run.message.isNotBlank()) {
                        Text(
                            run.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "done ${run.completedLegs}/${run.totalLegs} - failed ${run.failedLegs}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (run.rawTail.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Raw log")
                        MonoBlock(run.rawTail.joinToString("\n"))
                    }
                }
            }
        }
    }
}
