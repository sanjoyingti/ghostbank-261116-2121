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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
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
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

@Composable
fun HarvestRoute(container: AppContainer, onOpenTransfer: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()

    HarvestScreen(
        vm = vm,
        ui = ui,
        run = run,
        totals = totals,
        onOpenTransfer = onOpenTransfer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestScreen(
    vm: HarvestViewModel,
    ui: com.aibuilt.ghostbank.viewmodel.HarvestUi,
    run: com.aibuilt.ghostbank.engine.RunState,
    totals: Long,
    onOpenTransfer: () -> Unit
) {
    Scaffold(
        topBar = {
            GhostTopBar(title = "Harvest", actions = {
                IconButton(onClick = onOpenTransfer) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Transfer")
                }
                IconButton(onClick = { vm.resetHistory() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear history")
                }
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
                    SectionTitle("Micro total")
                    Text(
                        "${paiseLabel(totals)}  (${totals} paise booked)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "One paisa per account, once. Accounts already harvested are skipped by the registry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.start() },
                        enabled = !run.active && ui.selected.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("  Run ${ui.selected.size}")
                    }
                    OutlinedButton(
                        onClick = { vm.stop() },
                        enabled = run.active,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Text("  Stop")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.selectAllUnharvested() }) { Text("Select all unharvested") }
                    TextButton(onClick = { vm.clearSelection() }) { Text("Clear") }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Run")
                    KeyValue("State", if (run.active) "running" else "idle")
                    KeyValue("Progress", "${run.completedLegs}/${run.totalLegs}")
                    KeyValue("Failed", run.failedLegs.toString())
                    KeyValue("Stealth score", run.stealthScore.toString())
                    StageTimeline(run.ticks)
                }
            }

            item { SectionTitle("Accounts (${ui.accounts.size})") }

            if (ui.accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = "Nothing to harvest",
                        subtitle = "Save an account first, then come back"
                    )
                }
            } else {
                items(ui.accounts, key = { it.id }) { account ->
                    val harvested = ui.harvests.any {
                        it.accountId == account.id &&
                            it.state in setOf(HarvestState.SETTLED, HarvestState.FLOOR, HarvestState.HARVESTED)
                    }
                    val last = ui.harvests.firstOrNull { it.accountId == account.id }
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = account.id in ui.selected,
                                onCheckedChange = { vm.toggle(account.id) },
                                enabled = !harvested
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.holderLabel, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${account.accountNumber}  -  ${account.ifsc}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    account.bankName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (last != null) {
                                StatusPill(last.state.name, stateColor(last.state))
                            } else {
                                StatusPill("PENDING", MaterialTheme.colorScheme.outline)
                            }
                        }
                        if (last != null && last.reason.isNotBlank()) {
                            Text(
                                last.reason.take(220),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        last?.utr?.let {
                            Text("UTR $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item { SectionTitle("Raw log") }

            item {
                MonoBlock(
                    text = if (run.rawTail.isEmpty()) "nothing yet" else run.rawTail.joinToString("\n"),
                    maxHeight = 240
                )
            }
        }
    }
}

private fun stateColor(state: HarvestState): Color = when (state) {
    HarvestState.SETTLED, HarvestState.HARVESTED -> Color(0xFF38F0C0)
    HarvestState.FLOOR -> Color(0xFFFFC857)
    HarvestState.REJECTED -> Color(0xFFFF6B6B)
    else -> Color(0xFF9BB0C4)
}
