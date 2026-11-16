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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.HarvestEntity
import com.aibuilt.ghostbank.engine.RunState
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StageTimeline
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.HarvestUi
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

@Composable
fun HarvestRoute(container: AppContainer, onOpenTransfer: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    val totalPaise by vm.totals.collectAsStateWithLifecycle()

    HarvestScreen(
        ui = ui,
        run = run,
        totalPaise = totalPaise,
        onToggle = vm::toggle,
        onSelectAll = vm::selectAllUnharvested,
        onClear = vm::clearSelection,
        onStart = vm::start,
        onStop = vm::stop,
        onReset = vm::resetHistory,
        onOpenTransfer = onOpenTransfer
    )
}

private fun stateColor(state: HarvestState): Color? = when (state) {
    HarvestState.SETTLED, HarvestState.HARVESTED -> null
    HarvestState.FLOOR -> null
    HarvestState.REJECTED -> null
    else -> null
}

@Composable
fun HarvestScreen(
    ui: HarvestUi,
    run: RunState,
    totalPaise: Long,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onOpenTransfer: () -> Unit
) {
    val done = ui.harvests.filter {
        it.state == HarvestState.SETTLED || it.state == HarvestState.FLOOR || it.state == HarvestState.HARVESTED
    }
    val byAccount = done.associateBy { it.accountId }

    Scaffold(
        topBar = {
            GhostTopBar(title = "Harvest", actions = {
                TextButton(onClick = onOpenTransfer) {
                    Icon(Icons.Default.SwapVert, contentDescription = null)
                }
            })
        }
    ) { padding ->
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
                    SectionTitle("Micro-harvest ledger")
                    KeyValue("Harvested paise", "$totalPaise paise")
                    KeyValue("Settled accounts", "${done.size}")
                    KeyValue("Floored accounts", "${done.count { it.state == HarvestState.FLOOR }}")
                    Text(
                        "One paisa, once, per account. An account already settled is skipped forever by the dedupe registry, which is written before the request leaves.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Accounts (${ui.selected.size} selected)")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onSelectAll) { Text("Unharvested") }
                    TextButton(onClick = onClear) { Text("None") }
                }
            }

            item {
                if (run.active) {
                    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                        Text("Stop harvest")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        enabled = ui.selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Harvest 1 paisa each")
                    }
                }
            }

            if (ui.accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = "Nothing to harvest",
                        subtitle = "Add source accounts on the Account screen first."
                    )
                }
            }

            items(ui.accounts, key = { it.id }) { account ->
                val settled: HarvestEntity? = byAccount[account.id]
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = ui.selected.contains(account.id),
                            onCheckedChange = { onToggle(account.id) },
                            enabled = settled == null
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.holderLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${account.bankName} - ${account.accountNumber.takeLast(4)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (settled != null) {
                            StatusPill(
                                text = if (settled.state == HarvestState.FLOOR) "FLOOR" else "HARVESTED",
                                color = if (settled.state == HarvestState.FLOOR) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        } else {
                            StatusPill("PENDING", MaterialTheme.colorScheme.outline)
                        }
                    }
                    settled?.utr?.let {
                        KeyValue("UTR", it, MaterialTheme.colorScheme.primary)
                    }
                    if (settled != null) {
                        KeyValue("Paise", settled.amountPaise.toString())
                    }
                }
            }

            item { SectionTitle("Latest attempts") }

            if (ui.harvests.isEmpty()) {
                item {
                    Text(
                        "No attempts recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(ui.harvests, key = { it.id }) { harvest ->
                val color = when (harvest.state) {
                    HarvestState.SETTLED, HarvestState.HARVESTED -> MaterialTheme.colorScheme.primary
                    HarvestState.FLOOR -> MaterialTheme.colorScheme.tertiary
                    HarvestState.REJECTED -> MaterialTheme.colorScheme.error
                    HarvestState.SENT -> MaterialTheme.colorScheme.secondary
                    HarvestState.PENDING -> MaterialTheme.colorScheme.outline
                }
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(harvest.bankName, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        StatusPill(harvest.state.name, color)
                    }
                    Text(
                        harvest.reason.ifBlank { "no detail" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    harvest.utr?.let { KeyValue("UTR", it, MaterialTheme.colorScheme.primary) }
                }
            }

            item {
                TextButton(onClick = onReset) { Text("Clear harvest history") }
            }

            if (run.ticks.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Live pipeline")
                        StageTimeline(run.ticks)
                    }
                }
            }
        }
    }
}
