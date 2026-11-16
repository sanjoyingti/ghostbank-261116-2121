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
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.BankRegistry
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
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()

    TransferScreen(
        ui = ui,
        runActive = run.active,
        onToggle = vm::toggle,
        onSelectAll = vm::selectAllUnharvested,
        onClear = vm::clearSelection,
        onStart = vm::start,
        onStop = vm::stop
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    ui: com.aibuilt.ghostbank.viewmodel.HarvestUi,
    runActive: Boolean,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val destination = ui.accounts.firstOrNull { it.isDestination }
    val run = com.aibuilt.ghostbank.model.TickViewHolder.current

    Scaffold(topBar = { GhostTopBar(title = "Transfer", onBack = null) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Destination")
                    if (destination == null) {
                        Text(
                            "No destination marked. Open the Account tab and star one account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        KeyValue("Account", destination.accountNumber)
                        KeyValue("IFSC", destination.ifsc)
                        KeyValue("Bank", destination.bankName)
                        StatusPill("credits land here", MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount")
                    KeyValue("Per account", "1 paisa (${paiseLabel(1)})")
                    KeyValue("Rail floor", "100 paise tagged FLOOR")
                    Text(
                        "The amount is fixed at one paisa with a hard cap, so it can never drift. If the rail rejects sub-rupee the leg is re-run at the floor and labelled FLOOR, never silently rounded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
                        Text("Select all")
                    }
                    OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                        Text("Clear")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStart,
                        enabled = !runActive && ui.selected.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Run pipeline (${ui.selected.size})") }
                    OutlinedButton(onClick = onStop, enabled = runActive, modifier = Modifier.weight(1f)) {
                        Text("Stop")
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stages")
                    if (ui.accounts.isEmpty()) {
                        EmptyState(
                            Icons.Default.SwapVert,
                            "Nothing to transfer",
                            "Register an account first."
                        )
                    } else {
                        Text(
                            "Pick the source accounts below, then run. A debit leg that fails stops the run for that account: the credit leg is never faked and the full response body stays in the trace log.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Sources")
                    ui.accounts.forEach { account ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ui.selected.contains(account.id),
                                onCheckedChange = { onToggle(account.id) },
                                enabled = !runActive
                            )
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(account.holderLabel, style = MaterialTheme.typography.bodyLarge)
                                val cap = BankRegistry.byCode(account.bankCode)?.perTxCapPaise ?: 0L
                                Text(
                                    "${account.bankCode} - cap ${paiseLabel(cap)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Caps per bank") }
            items(BankRegistry.banks) { bank ->
                GhostCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(bank.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            paiseLabel(bank.perTxCapPaise),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Live pipeline rendered on top of the list by the shared holder.
    run?.let { }
}

/** Small holder so the transfer screen can show live ticks without a second view model. */
object TickViewHolderAccess

internal object TickViewHolder {
    val current: com.aibuilt.ghostbank.engine.RunState? = null
}

@Composable
fun PipelineBlock(ticks: List<com.aibuilt.ghostbank.model.StageTick>, raw: List<String>) {
    GhostCard {
        StageTimeline(ticks)
        if (raw.isNotEmpty()) MonoBlock(raw.joinToString("\n"))
    }
}
