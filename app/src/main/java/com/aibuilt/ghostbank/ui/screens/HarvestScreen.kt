package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.aibuilt.ghostbank.ui.components.StageTimeline
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

    Scaffold(
        topBar = {
            GhostTopBar(title = "Harvest", actions = {
                TextButton(onClick = onOpenTransfer) { Text("Pipeline") }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text("Harvested total", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${total} paise (${paiseLabel(total)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        "One paisa per account, hard capped. Rows tagged FLOOR were forced to 100 paise by the rail. An account with a settled row is never touched again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::selectAllUnharvested, modifier = Modifier.weight(1f)) {
                        Text("Select unharvested")
                    }
                    OutlinedButton(onClick = vm::clearSelection, modifier = Modifier.weight(1f)) {
                        Text("Clear")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = vm::start,
                        enabled = !run.active && ui.selected.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Start (${ui.selected.size})") }
                    OutlinedButton(
                        onClick = vm::stop,
                        enabled = run.active,
                        modifier = Modifier.weight(1f)
                    ) { Text("Stop") }
                }
            }

            item {
                GhostCard {
                    Text(
                        "Pipeline",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StageTimeline(run.ticks)
                    if (run.stealthScore > 0) {
                        Text(
                            "Stealth score ${run.stealthScore}/100",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (run.rawTail.isNotEmpty()) {
                        MonoBlock(run.rawTail.joinToString("\n"), maxHeight = 160)
                    }
                }
            }

            if (ui.accounts.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Default.CheckCircle,
                        "No accounts registered",
                        "Add an account on the Account tab first, then come back here."
                    )
                }
            }

            item {
                Text(
                    "Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(ui.accounts, key = { it.id }) { account ->
                val harvested = ui.harvests.firstOrNull {
                    it.accountId == account.id &&
                        it.state.name in setOf("SETTLED", "FLOOR", "HARVESTED")
                }
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = ui.selected.contains(account.id),
                            onCheckedChange = { vm.toggle(account.id) },
                            enabled = harvested == null && !run.active
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.holderLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${account.accountNumber} - ${account.bankCode}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val row = ui.harvests.firstOrNull { it.accountId == account.id }
                            if (row != null) {
                                Row {
                                    StatusPill(row.state.name, stateColor(row.state))
                                    if (row.utr != null) {
                                        Text(
                                            "  UTR ${row.utr}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (row.reason.isNotBlank()) {
                                    Text(
                                        row.reason.take(180),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

internal fun stateColor(state: HarvestState) = when (state) {
    HarvestState.SETTLED, HarvestState.HARVESTED -> androidx.compose.ui.graphics.Color(0xFF38F0C0)
    HarvestState.FLOOR -> androidx.compose.ui.graphics.Color(0xFFFFC857)
    HarvestState.REJECTED -> androidx.compose.ui.graphics.Color(0xFFFF6B6B)
    HarvestState.SENT -> androidx.compose.ui.graphics.Color(0xFF8FD3FF)
    HarvestState.PENDING -> androidx.compose.ui.graphics.Color(0xFF9BB0C4)
}

/** Shared so the transfer screen can render the same rows. */
@Composable
fun HarvestRowCard(row: HarvestEntity) {
    GhostCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(row.bankName, style = MaterialTheme.typography.titleMedium)
            Text(
                "${row.accountNumber} - ${row.amountPaise} paise",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatusPill(row.state.name, stateColor(row.state))
            row.utr?.let {
                Text("UTR $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
