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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.model.HarvestState
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
import com.aibuilt.ghostbank.viewmodel.HarvestUi
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

@Composable
fun HarvestRoute(container: AppContainer, onOpenTransfer: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    val total by vm.totals.collectAsStateWithLifecycle()

    HarvestScreen(
        ui = ui,
        running = run.active,
        totalPaise = total,
        runMessage = run.message,
        ticks = run.ticks,
        rawTail = run.rawTail,
        completed = run.completedLegs,
        failed = run.failedLegs,
        onToggle = vm::toggle,
        onSelectAll = vm::selectAllUnharvested,
        onClearSelection = vm::clearSelection,
        onStart = vm::start,
        onStop = vm::stop,
        onResetHistory = vm::resetHistory,
        onOpenTransfer = onOpenTransfer
    )
}

@Composable
fun HarvestScreen(
    ui: HarvestUi,
    running: Boolean,
    totalPaise: Long,
    runMessage: String,
    ticks: List<com.aibuilt.ghostbank.model.StageTick>,
    rawTail: List<String>,
    completed: Int,
    failed: Int,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onResetHistory: () -> Unit,
    onOpenTransfer: () -> Unit
) {
    Scaffold(
        topBar = {
            GhostTopBar(title = "Harvest", actions = {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.Checklist, contentDescription = "Select all")
                }
                IconButton(onClick = onOpenTransfer) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Transfer")
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
                    KeyValue("Total pulled", paiseLabel(totalPaise))
                    KeyValue("Rule", "1 paisa per account, never twice")
                    KeyValue("Cap", "Registry written before the request leaves")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onStart,
                            enabled = !running && ui.selected.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Text("  Harvest ${ui.selected.size}")
                        }
                        OutlinedButton(onClick = onStop, enabled = running, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text("  Stop")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClearSelection, modifier = Modifier.weight(1f)) {
                            Text("Clear selection")
                        }
                        OutlinedButton(onClick = onResetHistory, modifier = Modifier.weight(1f)) {
                            Text("Reset history")
                        }
                    }
                    if (runMessage.isNotBlank()) {
                        Text(runMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (running) {
                        Text(
                            "done $completed, failed $failed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (ticks.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Pipeline")
                        StageTimeline(ticks)
                    }
                }
            }

            if (rawTail.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Raw log")
                        MonoBlock(rawTail.joinToString("\n"))
                    }
                }
            }

            if (ui.accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = "Nothing to harvest",
                        subtitle = "Add accounts on the Account screen first."
                    )
                }
            } else {
                items(ui.accounts, key = { it.id }) { account ->
                    val harvest = ui.harvests.firstOrNull { it.accountId == account.id }
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ui.selected.contains(account.id),
                                onCheckedChange = { onToggle(account.id) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    maskAccount(account.accountNumber),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "${account.bankName} - ${account.ifsc}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                harvest?.utr?.let {
                                    Text(
                                        "UTR $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            when (harvest?.state) {
                                HarvestState.SETTLED, HarvestState.HARVESTED ->
                                    StatusPill("HARVESTED", MaterialTheme.colorScheme.primary)
                                HarvestState.FLOOR ->
                                    StatusPill("FLOOR", MaterialTheme.colorScheme.tertiary)
                                HarvestState.REJECTED ->
                                    StatusPill("REJECTED", MaterialTheme.colorScheme.error)
                                HarvestState.SENT ->
                                    StatusPill("SENT", MaterialTheme.colorScheme.secondary)
                                HarvestState.PENDING ->
                                    StatusPill("PENDING", MaterialTheme.colorScheme.outline)
                                null ->
                                    StatusPill("untouched", MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}
