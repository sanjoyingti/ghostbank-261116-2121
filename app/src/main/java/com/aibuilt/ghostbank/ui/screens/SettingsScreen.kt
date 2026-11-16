package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.ToggleRow
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

@Composable
fun SettingsRoute(container: AppContainer, onOpenTrace: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()

    SettingsScreen(
        vm = vm,
        proxyPool = settings.proxyPool.map { it.raw },
        torMode = settings.torMode,
        killSwitch = settings.killSwitch,
        exactPaisa = settings.exactOnePaisa,
        onOpenTrace = onOpenTrace
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    proxyPool: List<String>,
    torMode: Boolean,
    killSwitch: Boolean,
    exactPaisa: Boolean,
    onOpenTrace: () -> Unit
) {
    var draft by remember(proxyPool) { mutableStateOf(proxyPool.joinToString("\n")) }

    Scaffold(topBar = { GhostTopBar(title = "Settings") }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Exit pool")
                    Text(
                        "One entry per line: http://host:port or socks5://host:port. Requests rotate through the list and never repeat one while another is available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Proxy entries") },
                        minLines = 4,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { vm.saveProxies(draft) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save exits (${proxyPool.size} active)") }
                    ToggleRow("Tor mode", "Use Orbot on 127.0.0.1:9050", torMode, vm::setTor)
                    ToggleRow("Kill-switch", "Never send on the raw IP", killSwitch, vm::setKillSwitch)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount")
                    KeyValue("Harvest amount", if (exactPaisa) "exactly 1 paisa" else "rail default")
                    KeyValue("Hard cap", "one leg per account, ever")
                    ToggleRow("Exact one paisa", "Hard-cap the amount so it can never drift", exactPaisa, vm::setExactPaisa)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Local data")
                    Text(
                        "Nothing leaves the device: no analytics, no cloud, no telemetry SDK. The ledger and the raw log live in a private local database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.Icon(Icons.Default.ReceiptLong, contentDescription = null)
                        Text("  Open trace audit")
                    }
                    OutlinedButton(onClick = { vm.clearLogs() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear raw log")
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Registry rule")
                    Text(
                        "An endpoint may only carry a debit leg if it is flagged VERIFIED with live probe evidence. Invented SBI routes stay probe-only until they answer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("About")
                    Text(
                        "GhostBank 1.4 - IMPS P2A micro-harvest, Trace-Shield stealth, on-device only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
