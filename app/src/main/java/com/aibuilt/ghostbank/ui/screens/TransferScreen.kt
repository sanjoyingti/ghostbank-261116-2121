package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.HarvestViewModel

@Composable
fun TransferRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: HarvestViewModel = viewModel(factory = ghostFactory { HarvestViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    var amount by remember { mutableStateOf("1") }
    var sourceId by remember { mutableStateOf<Long?>(null) }

    Scaffold(topBar = { GhostTopBar(title = "Transfer pipeline", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GhostCard {
                SectionTitle("Source account")
                if (ui.accounts.isEmpty()) {
                    Text("Add an account first.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    ui.accounts.forEach { account ->
                        OutlinedButton(
                            onClick = { sourceId = account.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (sourceId == account.id) {
                                    "> ${account.holderLabel} - ${account.ifsc}"
                                } else {
                                    "${account.holderLabel} - ${account.ifsc}"
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() }.take(9) },
                    label = { Text("Amount in paise (1 = one paisa)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "The rail floor is Rs 1. If the bank refuses a sub-rupee leg it is retried at 100 paise and tagged FLOOR.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val paise = (amount.toLongOrNull() ?: 1L).coerceAtLeast(1L)
                            vm.startWith(listOfNotNull(sourceId), paise)
                        },
                        enabled = !run.active && sourceId != null
                    ) { Text("Run pipeline") }
                    OutlinedButton(onClick = { vm.stop() }, enabled = run.active) { Text("Stop") }
                }
            }

            GhostCard {
                SectionTitle("Run ${if (run.runId.isBlank()) "-" else run.runId.take(8)}")
                StatusPill(
                    text = if (run.active) "running" else if (run.runId.isBlank()) "idle" else "finished",
                    color = if (run.active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                )
                KeyValue("Stage", run.current.name.replace('_', ' '))
                KeyValue("Legs done", "${run.completedLegs} of ${run.totalLegs}")
                KeyValue("Failed legs", run.failedLegs.toString())
                KeyValue("Stealth score", run.stealthScore.toString())
                if (run.message.isNotBlank()) {
                    Text(run.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            GhostCard {
                SectionTitle("Stage timeline")
                if (run.ticks.isEmpty()) {
                    Text(
                        "Nothing has run yet. Each stage only advances when the server confirms it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    StageTimeline(run.ticks)
                }
            }

            GhostCard {
                SectionTitle("Raw log (kept, HTML bodies included)")
                MonoBlock(
                    text = if (run.rawTail.isEmpty()) "no lines yet" else run.rawTail.joinToString("\n"),
                    maxHeight = 320
                )
            }
        }
    }
}
