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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.AccountEntity
import com.aibuilt.ghostbank.data.db.HarvestEntity
import com.aibuilt.ghostbank.engine.RunState
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.paiseLabel
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

@Composable
fun HarvestRoute(container: AppContainer, onOpenTransfer: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    val total by vm.totals.collectAsStateWithLifecycle()

    LaunchedEffect(ui.toast) {
        if (ui.toast != null) vm.clearToast()
    }

    HarvestScreen(
        accounts = ui.accounts,
        harvests = ui.harvests,
        selected = ui.selected,
        run = run,
        totalPaise = total,
        toast = ui.toast,
        onToggle = vm::toggle,
        onSelectAll = vm::selectAllUnharvested,
        onClear = vm::clearSelection,
        onStart = vm::start,
        onStop = vm::stop,
        onResetHistory = vm::resetHistory,
        onOpenTransfer = onOpenTransfer
    )
}

@Composable
fun HarvestScreen(
    accounts: List<AccountEntity>,
    harvests: List<HarvestEntity>,
    selected: Set<Long>,
    run: RunState,
    totalPaise: Long,
    toast: String?,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onResetHistory: () -> Unit,
    onOpenTransfer: () -> Unit
) {
    Scaffold(
        topBar = {
            GhostTopBar(title = "Harvest", actions = {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Counter")
                    Text(
                        "$totalPaise paise harvested in total",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "One paisa per account, once, ever. A bank that refuses sub-rupee is tagged FLOOR at ${paiseLabel(100)} and counted in paise.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            toast?.let {
                item {
                    GhostCard {
                        Text(it, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStart, enabled = !run.active, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text("  Harvest")
                    }
                    OutlinedButton(onClick = onStop, enabled = run.active, modifier = Modifier.weight(1f)) {
                        Text("Stop")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSelectAll) { Text("Select unharvested") }
                    TextButton(onClick = onClear) { Text("Clear") }
                    TextButton(onClick = onResetHistory) { Text("Reset history") }
                }
            }

            item {
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(
                            "Run ${run.completedLegs}/${run.totalLegs} legs, ${run.failedLegs} failed",
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(
                            text = if (run.active) "running" else "idle",
                            color = if (run.active) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        run.message.ifBlank { "Nothing running." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MonoBlock(
                        if (run.rawTail.isEmpty()) "no traffic yet" else run.rawTail.joinToString("\n"),
                        maxHeight = 180
                    )
                }
            }

            item { SectionTitle("Accounts (${accounts.size})") }

            if (accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = "Nothing to harvest",
                        subtitle = "Add accounts on the Account screen first."
                    )
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    val last = harvests.firstOrNull { it.accountId == account.id }
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = account.id in selected,
                                onCheckedChange = { onToggle(account.id) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    maskAccount(account.accountNumber),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    account.bankName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                last?.let {
                                    Text(
                                        "${it.state.name} · ${it.amountPaise} paise · utr ${it.utr ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = statusColor(it.state)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusColor(state: HarvestState) = when (state) {
    HarvestState.SETTLED, HarvestState.HARVESTED -> MaterialTheme.colorScheme.primary
    HarvestState.FLOOR -> MaterialTheme.colorScheme.tertiary
    HarvestState.REJECTED -> MaterialTheme.colorScheme.error
    HarvestState.SENT -> MaterialTheme.colorScheme.secondary
    HarvestState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
}
