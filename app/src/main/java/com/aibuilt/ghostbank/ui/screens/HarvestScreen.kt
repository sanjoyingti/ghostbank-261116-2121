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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
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
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.paiseLabel
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel
import kotlinx.coroutines.delay

@Composable
fun HarvestRoute(container: AppContainer, onOpenTransfer: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    val totalPaise by vm.totals.collectAsStateWithLifecycle()

    LaunchedEffect(ui.toast) {
        if (ui.toast != null) {
            delay(2500)
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = {
            GhostTopBar(title = "Harvest", actions = {
                androidx.compose.material3.IconButton(onClick = onOpenTransfer) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Transfer")
                }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("One paisa, once, per account")
                    KeyValue("Collected so far", "${paiseLabel(totalPaise)} ($totalPaise paise)")
                    KeyValue("Mode", "exact 1 paisa, hard-capped")
                    Text(
                        "The registry is written before the request leaves, so a crash mid-run can never harvest the same account twice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (ui.toast != null) {
                item {
                    GhostCard {
                        Text(ui.toast!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                        TextButton(onClick = vm::clearToast) { Text("Dismiss") }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { vm.startWith(ui.selected.toList(), 1L) },
                        enabled = !run.active && ui.selected.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text(" Harvest ${ui.selected.size}")
                    }
                    OutlinedButton(onClick = vm::stop, enabled = run.active, modifier = Modifier.weight(1f)) {
                        Text("Stop")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::selectAllUnharvested) { Text("Select all unharvested") }
                    OutlinedButton(onClick = vm::clearSelection) { Text("Clear") }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Accounts")
                    StatusPill("${ui.selected.size} queued")
                    if (run.active) StatusPill("running", MaterialTheme.colorScheme.primary)
                }
            }

            if (ui.accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = "Nothing to harvest",
                        subtitle = "Add an account on the Accounts tab and it shows up here."
                    )
                }
            } else {
                items(ui.accounts) { account ->
                    val state = vm.stateOf(account.id)
                    val selected = ui.selected.contains(account.id)
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle(maskAccount(account.accountNumber))
                            val color = when (state) {
                                "HARVESTED", "SETTLED" -> MaterialTheme.colorScheme.primary
                                "FLOOR" -> MaterialTheme.colorScheme.tertiary
                                "REJECTED" -> MaterialTheme.colorScheme.error
                                "SENT" -> MaterialTheme.colorScheme.secondary
                                "PENDING" -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.outline
                            }
                            StatusPill(state ?: "not run", color)
                            if (selected) StatusPill("queued")
                        }
                        KeyValue("IFSC", account.ifsc)
                        KeyValue("Bank", account.bankName)
                        val utr = ui.harvests.firstOrNull { it.accountId == account.id }?.utr
                        if (utr != null) KeyValue("UTR", utr, MaterialTheme.colorScheme.primary)
                        OutlinedButton(onClick = { vm.toggle(account.id) }) {
                            Text(if (selected) "Remove" else "Queue")
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = vm::resetHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear harvest history (re-arms every account)")
                }
            }
        }
    }
}
