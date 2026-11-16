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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.RegistryAudit
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.ToggleRow
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

@Composable
fun SettingsRoute(container: AppContainer, onOpenTrace: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val vas by vm.virtualAccounts.collectAsStateWithLifecycle()

    var proxyText by remember { mutableStateOf(settings.proxyPool.joinToString("\n") { it.raw }) }
    var vaNumber by remember { mutableStateOf("") }
    var vaIfsc by remember { mutableStateOf("") }
    var vaLabel by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            GhostTopBar(title = "Settings", actions = {
                IconButton(onClick = onOpenTrace) {
                    Icon(Icons.Default.BugReport, contentDescription = "Trace audit")
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
                    SectionTitle("Exit pool")
                    Text(
                        "One entry per line, host:port. Prefix with socks5:// for a SOCKS exit. Traffic rotates through the pool; with the kill-switch armed nothing leaves on the raw IP.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = proxyText,
                        onValueChange = { proxyText = it },
                        label = { Text("Proxy list") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            vm.saveProxies(proxyText.split("\n").map { it.trim() }.filter { it.isNotEmpty() })
                        }) { Text("Save exits") }
                        OutlinedButton(onClick = { proxyText = ""; vm.saveProxies(emptyList()) }) {
                            Text("Clear")
                        }
                    }
                    KeyValue("Configured", "${settings.proxyPool.size}")
                }
            }

            item {
                GhostCard {
                    SectionTitle("Velocity caps")
                    KeyValue("Requests / minute", settings.maxRequestsPerMinute.toString())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            vm.setCaps(settings.maxRequestsPerMinute - 5, settings.maxAccountsPerHour)
                        }) { Text("-5") }
                        Button(onClick = {
                            vm.setCaps(settings.maxRequestsPerMinute + 5, settings.maxAccountsPerHour)
                        }) { Text("+5") }
                    }
                    KeyValue("Accounts / hour", settings.maxAccountsPerHour.toString())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            vm.setCaps(settings.maxRequestsPerMinute, settings.maxAccountsPerHour - 5)
                        }) { Text("-5") }
                        Button(onClick = {
                            vm.setCaps(settings.maxRequestsPerMinute, settings.maxAccountsPerHour + 5)
                        }) { Text("+5") }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Daily window")
                    KeyValue("From", "${settings.windowStartHour}:00")
                    KeyValue("To", "${settings.windowEndHour}:00")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            vm.setWindow((settings.windowStartHour - 1).coerceIn(0, 23), settings.windowEndHour)
                        }) { Text("start -1h") }
                        Button(onClick = {
                            vm.setWindow((settings.windowStartHour + 1).coerceIn(0, 23), settings.windowEndHour)
                        }) { Text("start +1h") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            vm.setWindow(settings.windowStartHour, (settings.windowEndHour - 1).coerceIn(1, 24))
                        }) { Text("end -1h") }
                        Button(onClick = {
                            vm.setWindow(settings.windowStartHour, (settings.windowEndHour + 1).coerceIn(1, 24))
                        }) { Text("end +1h") }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Rotating virtual destinations")
                    Text(
                        "Credits alternate across these so the digits on the rail are not one repeating pattern.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = vaNumber,
                        onValueChange = { vaNumber = it.filter { c -> c.isDigit() }.take(18) },
                        label = { Text("Virtual account number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vaIfsc,
                        onValueChange = { vaIfsc = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(11) },
                        label = { Text("IFSC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vaLabel,
                        onValueChange = { vaLabel = it.take(30) },
                        label = { Text("Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        vm.addVirtualAccount(vaNumber, vaIfsc, vaLabel)
                        vaNumber = ""; vaIfsc = ""; vaLabel = ""
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add")
                    }
                }
            }

            items(vas, key = { it.id }) { va ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(maskAccount(va.accountNumber), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(8.dp))
                        StatusPill("used ${va.uses}x", MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.weight(1f))
                        Switch(checked = va.active, onCheckedChange = { vm.toggleVirtualAccount(va.id, it) })
                        IconButton(onClick = { vm.removeVirtualAccount(va.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    KeyValue("IFSC", va.ifsc)
                    KeyValue("Label", va.label)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Behaviour")
                    ToggleRow("Exact 1 paisa", "Amount locked to 1 paisa, hard capped", settings.exactOnePaisa) { vm.setExactPaisa(it) }
                    ToggleRow("Kill-switch", "Never send on the raw IP", settings.killSwitch) { vm.setKillSwitch(it) }
                    ToggleRow("DNS over HTTPS", "Resolve through HTTPS, not the ISP", settings.dohEnabled) { vm.setDoh(it) }
                    ToggleRow("Tor mode", "Use Orbot as the exit", settings.torMode) { vm.setTor(it) }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Housekeeping")
                    TextButton(onClick = vm::clearLogs) { Text("Clear trace log") }
                    val issues = RegistryAudit.violations()
                    Text(
                        if (issues.isEmpty()) "Registry self-check clean: ${RegistryAudit.violations().size} violations."
                        else "Registry self-check: ${issues.size} violation(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (issues.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        "On-device only: encrypted local ledger, no analytics, no telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
