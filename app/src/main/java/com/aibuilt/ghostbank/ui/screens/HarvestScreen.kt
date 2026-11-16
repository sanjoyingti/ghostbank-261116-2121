package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
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
                    SectionTitle("Micro-harvest ledger")
                    KeyValue("Total taken", "${total} paise (${paiseLabel(total)})")
                    KeyValue("Accounts saved", ui.accounts.size.toString())
                    KeyValue("Selected", ui.selected.size.toString())
                    Text(
                        "One paisa per account, once. The rail floor is one rupee, so a refusal upgrades that leg to 100 paise and tags it FLOOR.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (run.active) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "Running ${run.completedLegs}/${run.totalLegs} legs, ${run.failedLegs} failed",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = vm::start,
                            enabled = !run.active,
                            modifier = Modifier.weight(1f)
                        ) { Text("Harvest 1 paisa") }
                        OutlinedButton(
                            onClick = vm::stop,
                            enabled = run.active,
                            modifier = Modifier.weight(1f)
                        ) { Text("Stop") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = vm::selectAllUnharvested) { Text("Select all unharvested") }
                        TextButton(onClick = vm::clearSelection) { Text("Clear") }
                        TextButton(onClick = vm::resetHistory) { Text("Reset history") }
                    }
                }
            }

            item { SectionTitle("Accounts") }

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
                    val state = vm.stateOf(account.id)
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ui.selected.contains(account.id),
                                onCheckedChange = { vm.toggle(account.id) }
                            )
                            Text(
                                text = "${account.bankName} ${maskAccount(account.accountNumber)}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (state == HarvestState.SETTLED || state == HarvestState.HARVESTED) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "harvested",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            StatusPill(
                                text = state?.name ?: "NEW",
                                color = when (state) {
                                    HarvestState.SETTLED, HarvestState.HARVESTED -> MaterialTheme.colorScheme.primary
                                    HarvestState.FLOOR -> MaterialTheme.colorScheme.tertiary
                                    HarvestState.REJECTED -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                        ui.harvests.firstOrNull { it.accountId == account.id }?.let { row ->
                            KeyValue("IFSC", row.ifsc)
                            KeyValue("Amount", "${row.amountPaise} paise")
                            row.utr?.let { KeyValue("UTR", it) }
                            if (row.reason.isNotBlank()) KeyValue("Reason", row.reason.take(120))
                        }
                    }
                }
            }
        }
    }
}
