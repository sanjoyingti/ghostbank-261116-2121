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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.AccountEntity
import com.aibuilt.ghostbank.model.StageTick
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StageTimeline
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.TransferUi
import com.aibuilt.ghostbank.viewmodel.TransferViewModel

@Composable
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: TransferViewModel = viewModel(factory = ghostFactory { TransferViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()

    TransferScreen(
        ui = ui,
        accounts = accounts,
        destination = destination,
        running = run.active,
        message = run.message,
        ticks = run.ticks,
        rawTail = run.rawTail,
        onAmount = vm::setAmount,
        onOnePaisa = vm::setOnePaisa,
        onSource = vm::selectSource,
        onStart = vm::start,
        onStop = vm::stop,
        onBack = onBack
    )
}

@Composable
fun TransferScreen(
    ui: TransferUi,
    accounts: List<AccountEntity>,
    destination: AccountEntity?,
    running: Boolean,
    message: String,
    ticks: List<StageTick>,
    rawTail: List<String>,
    onAmount: (String) -> Unit,
    onOnePaisa: () -> Unit,
    onSource: (Long) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { GhostTopBar(title = "Transfer", onBack = onBack) }) { padding ->
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
                    SectionTitle("Destination")
                    if (destination == null) {
                        Text(
                            "No destination account marked yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        KeyValue("Account", maskAccount(destination.accountNumber))
                        KeyValue("IFSC", destination.ifsc)
                        KeyValue("Bank", destination.bankName)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount")
                    OutlinedTextField(
                        value = ui.amountInput,
                        onValueChange = onAmount,
                        label = { Text("Rupees") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    KeyValue("Exact paise", ui.amountPaise.toString())
                    TextButton(onClick = onOnePaisa) { Text("Use 1 paisa") }
                    ui.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onStart, enabled = !running, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.SwapVert, contentDescription = null)
                            Text("  Run pipeline")
                        }
                        OutlinedButton(onClick = onStop, enabled = running, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text("  Stop")
                        }
                    }
                }
            }

            item { SectionTitle("Source accounts") }

            if (accounts.isEmpty()) {
                item {
                    Text(
                        "No accounts saved. Add one on the Account screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = ui.sourceId == account.id,
                                onClick = { onSource(account.id) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(maskAccount(account.accountNumber), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${account.bankName} - ${account.ifsc}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (account.isDestination) {
                                StatusPill("destination", MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            if (message.isNotBlank()) {
                item { MonoBlock("last: $message") }
            }

            if (ticks.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Pipeline stages")
                        StageTimeline(ticks)
                    }
                }
            }

            if (rawTail.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Raw log (full bodies kept)")
                        MonoBlock(rawTail.joinToString("\n"), maxHeight = 320)
                    }
                }
            }
        }
    }
}
