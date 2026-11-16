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
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StageTimeline
import com.aibuilt.ghostbank.ui.components.paiseLabel
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.AccountViewModel
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

private fun parsePaise(text: String): Long {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 1L
    val parts = trimmed.split('.')
    val rupees = parts[0].filter { it.isDigit() }.toLongOrNull() ?: 0L
    val paise = parts.getOrNull(1)?.filter { it.isDigit() }?.take(2)?.padEnd(2, '0')?.toLongOrNull() ?: 0L
    return (rupees * 100L + paise).coerceAtLeast(1L)
}

@Composable
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val accountsVm: AccountViewModel = viewModel(factory = ghostFactory { AccountViewModel(container) })
    val harvestVm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })

    val accounts by accountsVm.accounts.collectAsStateWithLifecycle()
    val destination by accountsVm.destination.collectAsStateWithLifecycle()
    val run by harvestVm.run.collectAsStateWithLifecycle()

    var amountText by remember { mutableStateOf("0.01") }
    var sourceId by remember { mutableStateOf<Long?>(null) }

    val paise = parsePaise(amountText)

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
                            "No destination set. Mark one on the Account screen first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        KeyValue("Account", destination!!.accountNumber)
                        KeyValue("Bank", destination!!.bankName)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }.take(12) },
                        label = { Text("Rupees (0.01 = one paisa)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    KeyValue("Exact amount", "${paiseLabel(paise)} (${paise} paise)")
                    Text(
                        "If the rail refuses sub-rupee the leg is retried at the 100 paise floor and tagged FLOOR.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { harvestVm.startWith(listOfNotNull(sourceId), paise) },
                        enabled = !run.active && sourceId != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("Run pipeline") }
                    OutlinedButton(
                        onClick = harvestVm::stop,
                        enabled = run.active,
                        modifier = Modifier.weight(1f)
                    ) { Text("Stop") }
                }
            }

            if (run.active) {
                item {
                    Column {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(run.current.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (run.ticks.isNotEmpty()) {
                item {
                    GhostCard {
                        SectionTitle("Stages")
                        StageTimeline(run.ticks)
                    }
                }
            }

            if (run.rawTail.isNotEmpty()) {
                item { MonoBlock(run.rawTail.joinToString("\n")) }
            }

            item { SectionTitle("Source account") }

            items(accounts, key = { it.id }) { account ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = sourceId == account.id,
                            onClick = { sourceId = account.id }
                        )
                        Column {
                            Text(account.holderLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${account.accountNumber.takeLast(4)} - ${account.bankName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
