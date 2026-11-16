package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
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

private fun stateColor(state: HarvestState, primary: androidx.compose.ui.graphics.Color, error: androidx.compose.ui.graphics.Color, tertiary: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color =
    when (state) {
        HarvestState.SETTLED, HarvestState.HARVESTED -> primary
        HarvestState.FLOOR -> tertiary
        HarvestState.REJECTED -> error
        else -> tertiary
    }

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
                    SectionTitle("Micro-harvest")
                    Text(
                        "One paisa, once per account. The dedupe registry is written before the request leaves, so an account is never taken twice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    KeyValue("Ledger total", "${paiseLabel(total)} = $total paise")
                    KeyValue("Selected", "${ui.selected.size} account(s)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = vm::start,
                            enabled = !run.active && ui.selected.isNotEmpty()
                        ) { Text("Harvest 1 paisa each") }
                        OutlinedButton(onClick = vm::selectAllUnharvested, enabled = !run.active) {
                            Text("Select all")
                        }
                        TextButton(onClick = vm::clearSelection, enabled = !run.active) { Text("Clear") }
                    }
                    if (run.active) {
                        Text("running... ${run.current.name}", color = MaterialTheme.colorScheme.secondary)
                    }
                    ui.toast?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Accounts (${ui.accounts.size})")
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = vm::resetHistory) { Text("Reset history") }
                }
            }

            if (ui.accounts.isEmpty()) {
                item {
                    EmptyState(Icons.Default.Download, "Nothing to harvest", "Add accounts on the Account screen first.")
                }
            }

            items(ui.accounts, key = { it.id }) { account ->
                val state = ui.harvests.firstOrNull { it.accountId == account.id }?.state
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = ui.selected.contains(account.id),
                            onCheckedChange = { vm.toggle(account.id) }
                        )
                        Column2(maskAccount(account.accountNumber), account.bankName)
                        Spacer(Modifier.weight(1f))
                        when (state) {
                            null -> StatusPill("PENDING", MaterialTheme.colorScheme.outline)
                            else -> StatusPill(
                                state.name,
                                stateColor(
                                    state,
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        }
                    }
                    if (state == HarvestState.REJECTED) {
                        val reason = ui.harvests.firstOrNull { it.accountId == account.id }?.reason
                        if (!reason.isNullOrBlank()) {
                            Text(
                                reason.take(300),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    val utr = ui.harvests.firstOrNull { it.accountId == account.id }?.utr
                    if (utr != null) KeyValue("UTR", utr)
                    KeyValue("IFSC", account.ifsc)
                }
            }
        }
    }
}

@Composable
private fun Column2(title: String, subtitle: String) {
    androidx.compose.foundation.layout.Column(modifier = Modifier.padding(start = 4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
