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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
    val settings by vm.pool.collectAsStateWithLifecycle()

    TransferScreen(
        vm = vm,
        selected = ui.selected,
        accounts = ui.accounts,
        run = run,
        exitConfigured = settings.exitConfigured,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    vm: HarvestViewModel,
    selected: Set<Long>,
    accounts: List<com.aibuilt.ghostbank.data.db.AccountEntity>,
    run: com.aibuilt.ghostbank.engine.RunState,
    exitConfigured: Boolean,
    onBack: () -> Unit
) {
    Scaffold(topBar = { GhostTopBar(title = "Transfer", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Rail")
                    KeyValue("Route", "IMPS P2A")
                    KeyValue("Amount", "1 paisa (floor ${paiseLabel(100)} if the rail refuses)")
                    KeyValue("Credit leg", "only after the debit confirms")
                    if (!exitConfigured) {
                        Text(
                            "No exit configured - the kill-switch will hold every request.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (selected.isEmpty() && accounts.isNotEmpty()) {
                                vm.toggle(accounts.first().id)
                            }
                            vm.start()
                        },
                        enabled = !run.active && accounts.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("  Run pipeline")
                    }
                    OutlinedButton(
                        onClick = { vm.stop() },
                        enabled = run.active,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Stop, contentDescription = null)
                        Text("  Stop")
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Pipeline")
                    KeyValue("Run", run.runId.ifBlank { "not started" })
                    KeyValue("Stealth score", run.stealthScore.toString())
                    StageTimeline(run.ticks)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Choose the source account")
                    if (accounts.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.PlayArrow,
                            title = "No accounts yet",
                            subtitle = "Add an account on the Accounts screen first"
                        )
                    }
                }
            }

            items(accounts, key = { it.id }) { account ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = account.id in selected,
                            onClick = { vm.toggle(account.id) }
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
                        StatusPill(if (account.id in selected) "selected" else "idle")
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Raw log (last lines)")
                    Text(run.message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                MonoBlock(
                    text = if (run.rawTail.isEmpty()) "nothing yet" else run.rawTail.joinToString("\n"),
                    maxHeight = 260
                )
            }
        }
    }
}
