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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    val total by vm.totals.collectAsStateWithLifecycle()

    LaunchedEffect(ui.toast) {
        if (ui.toast != null) vm.clearToast()
    }

    Scaffold(topBar = { GhostTopBar(title = "Harvest") }) { padding ->
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
                    SectionTitle("Micro ledger")
                    KeyValue("Total harvested", paiseLabel(total))
                    KeyValue("Accounts in registry", ui.accounts.size.toString())
                    Text(
                        "One paisa per account, once ever. The dedupe registry is written before the request leaves.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::selectAllUnharvested, modifier = Modifier.weight(1f)) {
                        Text("Select new")
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
                        enabled = !run.active,
                        modifier = Modifier.weight(1f)
                    ) { Text("Run 1 paisa (${ui.selected.size})") }
                    OutlinedButton(
                        onClick = vm::stop,
                        enabled = run.active,
                        modifier = Modifier.weight(1f)
                    ) { Text("Stop") }
                }
            }

            if (run.active) {
                item {
                    Column {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "leg ${run.completedLegs}/${run.totalLegs} - ${run.current.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (run.ticks.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Pipeline")
                        StageTimeline(run.ticks)
                        StatusPill(
                            "failures ${run.failedLegs}",
                            if (run.failedLegs == 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
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

            item { SectionTitle("Accounts") }

            if (ui.accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = "Nothing to harvest",
                        subtitle = "Add an account on the Account screen first."
                    )
                }
            }

            items(ui.accounts, key = { it.id }) { account ->
                val state = ui.harvests.firstOrNull { it.accountId == account.id }?.state?.name
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = ui.selected.contains(account.id),
                            onCheckedChange = { vm.toggle(account.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.holderLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${account.accountNumber.takeLast(4)} - ${account.bankName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (state != null) {
                            StatusPill(
                                state,
                                when (state) {
                                    "HARVESTED", "SETTLED" -> MaterialTheme.colorScheme.primary
                                    "FLOOR" -> MaterialTheme.colorScheme.tertiary
                                    "REJECTED" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = onOpenTransfer, modifier = Modifier.fillMaxWidth()) {
                    Text("Open free-amount transfer")
                }
            }
        }
    }
}
