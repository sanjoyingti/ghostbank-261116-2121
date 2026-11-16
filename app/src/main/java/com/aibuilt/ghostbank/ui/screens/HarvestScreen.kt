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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.HarvestEntity
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StageTimeline
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

@Composable
fun HarvestRoute(container: AppContainer, onOpenTransfer: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    val total by vm.totals.collectAsStateWithLifecycle()

    HarvestScreen(
        accountRows = ui.accounts,
        harvests = ui.harvests,
        selected = ui.selected,
        totalPaise = total,
        running = run.active,
        ticks = run.ticks,
        message = run.message,
        rawTail = run.rawTail,
        toast = ui.toast,
        onToggle = vm::toggle,
        onSelectAll = vm::selectAllUnharvested,
        onClearSelection = vm::clearSelection,
        onStart = vm::start,
        onStop = vm::stop,
        onReset = vm::resetHistory,
        onClearToast = vm::clearToast,
        onOpenTransfer = onOpenTransfer
    )
}

@Composable
fun HarvestScreen(
    accountRows: List<com.aibuilt.ghostbank.data.db.AccountEntity>,
    harvests: List<HarvestEntity>,
    selected: Set<Long>,
    totalPaise: Long,
    running: Boolean,
    ticks: List<com.aibuilt.ghostbank.model.StageTick>,
    message: String,
    rawTail: List<String>,
    toast: String?,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onClearToast: () -> Unit,
    onOpenTransfer: () -> Unit
) {
    val harvestedIds = harvests
        .filter { it.state == HarvestState.HARVESTED || it.state == HarvestState.FLOOR || it.state == HarvestState.SETTLED }
        .map { it.accountId }
        .toSet()

    Scaffold(
        topBar = {
            GhostTopBar(title = "Harvest", actions = {
                IconButton(onClick = onOpenTransfer) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Transfer")
                }
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Download, contentDescription = "Reset history")
                }
            })
        }
    ) { padding ->
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
                    SectionTitle("1 paisa per account, once")
                    Text(
                        "Total harvested: ${totalPaise} paise (kept exact). An account is written as HARVESTED before the request leaves, so nothing is taken twice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onStart, enabled = !running, modifier = Modifier.weight(1f)) {
                            Text("Start run (${selected.size})")
                        }
                        OutlinedButton(onClick = onStop, enabled = running, modifier = Modifier.weight(1f)) {
                            Text("Stop")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onSelectAll) { Text("Select unharvested") }
                        TextButton(onClick = onClearSelection) { Text("Clear") }
                    }
                    toast?.let {
                        Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onClearToast) { Text("Dismiss") }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Live pipeline")
                    Text(
                        message.ifBlank { "Idle" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StageTimeline(ticks)
                    MonoBlock(
                        text = if (rawTail.isEmpty()) "no traffic yet" else rawTail.joinToString("\n"),
                        maxHeight = 200
                    )
                }
            }

            item { SectionTitle("Accounts (${accountRows.size})") }

            if (accountRows.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = "No accounts to harvest",
                        subtitle = "Add accounts on the Account tab, then come back."
                    )
                }
            }

            items(accountRows, key = { it.id }) { account ->
                val already = account.id in harvestedIds
                val last = harvests.firstOrNull { it.accountId == account.id }
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = account.id in selected,
                            onCheckedChange = { onToggle(account.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.holderLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${account.bankName} - ${account.accountNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (already) {
                            StatusPill("HARVESTED", MaterialTheme.colorScheme.primary)
                        } else if (last != null) {
                            StatusPill(last.state.name, MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    if (last != null) {
                        Text(
                            "status ${last.state.name}${last.utr?.let { " utr $it" } ?: ""}${if (last.reason.isBlank()) "" else " - ${last.reason.take(140)}"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
