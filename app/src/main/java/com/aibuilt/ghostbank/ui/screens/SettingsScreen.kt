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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.VirtualAccountEntity
import com.aibuilt.ghostbank.data.settings.GhostSettings
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
    val virtualAccounts by vm.virtualAccounts.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        virtualAccounts = virtualAccounts,
        onProxies = vm::saveProxies,
        onDoh = vm::setDoh,
        onKillSwitch = vm::setKillSwitch,
        onWindow = vm::setWindow,
        onDiurnal = vm::setDiurnal,
        onCaps = vm::setCaps,
        onFragmentation = vm::setFragmentation,
        onExactPaisa = vm::setExactPaisa,
        onAddVa = vm::addVirtualAccount,
        onRemoveVa = vm::removeVirtualAccount,
        onToggleVa = vm::toggleVirtualAccount,
        onOpenTrace = onOpenTrace,
        onClearLogs = vm::clearLogs
    )
}

@Composable
fun SettingsScreen(
    settings: GhostSettings,
    virtualAccounts: List<VirtualAccountEntity>,
    onProxies: (List<String>) -> Unit,
    onDoh: (Boolean) -> Unit,
    onKillSwitch: (Boolean) -> Unit,
    onWindow: (Int, Int) -> Unit,
    onDiurnal: (Boolean) -> Unit,
    onCaps: (Int, Int) -> Unit,
    onFragmentation: (Boolean) -> Unit,
    onExactPaisa: (Boolean) -> Unit,
    onAddVa: (String, String, String) -> Unit,
    onRemoveVa: (Long) -> Unit,
    onToggleVa: (Long, Boolean) -> Unit,
    onOpenTrace: () -> Unit,
    onClearLogs: () -> Unit
) {
    var proxyText by remember(settings.proxyPool) {
        mutableStateOf(settings.proxyPool.joinToString("\n") { it.raw })
    }
    var vaNumber by remember { mutableStateOf("") }
    var vaIfsc by remember { mutableStateOf("") }
    var vaLabel by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            GhostTopBar(title = "Settings", actions = {
                IconButton(onClick = onOpenTrace) {
                    Icon(Icons.Default.History, contentDescription = "Trace audit")
                }
            })
        }
    ) { padding ->
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
                    SectionTitle("Proxy pool")
                    Text(
                        "One exit per line: http://host:port or socks5://host:port. Rotation picks the next entry each call.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = proxyText,
                        onValueChange = { proxyText = it },
                        label = { Text("Exits") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onProxies(proxyText.split("\n").map { it.trim() }.filter { it.isNotEmpty() })
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Save exits (${settings.proxyPool.size})") }
                        OutlinedButton(
                            onClick = {
                                proxyText = ""
                                onProxies(emptyList())
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Clear") }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing")
                    ToggleRow(
                        title = "Diurnal window",
                        subtitle = "Prefer daytime requests",
                        checked = settings.diurnalWindow,
                        onChange = onDiurnal
                    )
                    Text(
                        "Window ${settings.windowStartHour}:00 to ${settings.windowEndHour}:00",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Start hour", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = settings.windowStartHour.toFloat(),
                        onValueChange = { onWindow(it.toInt().coerceIn(0, 23), settings.windowEndHour) },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                    Text("End hour", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = settings.windowEndHour.toFloat(),
                        onValueChange = { onWindow(settings.windowStartHour, it.toInt().coerceIn(0, 23)) },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Velocity caps")
                    KeyValue("Requests / minute", settings.maxRequestsPerMinute.toString())
                    Slider(
                        value = settings.maxRequestsPerMinute.toFloat(),
                        onValueChange = { onCaps(it.toInt().coerceIn(1, 300), settings.maxAccountsPerHour) },
                        valueRange = 1f..300f
                    )
                    KeyValue("Accounts / hour", settings.maxAccountsPerHour.toString())
                    Slider(
                        value = settings.maxAccountsPerHour.toFloat(),
                        onValueChange = { onCaps(settings.maxRequestsPerMinute, it.toInt().coerceIn(1, 500)) },
                        valueRange = 1f..500f
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Money rules")
                    ToggleRow(
                        title = "Exact 1 paisa",
                        subtitle = "Hard cap so the amount can never drift",
                        checked = settings.exactOnePaisa,
                        onChange = onExactPaisa
                    )
                    ToggleRow(
                        title = "Amount fragmentation",
                        subtitle = "Split larger pulls across virtual accounts",
                        checked = settings.fragmentation,
                        onChange = onFragmentation
                    )
                    ToggleRow(
                        title = "DNS over HTTPS",
                        subtitle = "Resolve names inside the tunnel",
                        checked = settings.dohEnabled,
                        onChange = onDoh
                    )
                    ToggleRow(
                        title = "Kill-switch",
                        subtitle = "Never send on the raw IP",
                        checked = settings.killSwitch,
                        onChange = onKillSwitch
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Virtual destination accounts (${virtualAccounts.size})")
                    Text(
                        "Credits can land on a rotating virtual account so your real digits stop being the pattern.",
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
                        label = { Text("Label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onAddVa(vaNumber, vaIfsc, vaLabel)
                            vaNumber = ""
                            vaIfsc = ""
                            vaLabel = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("  Add virtual account")
                    }
                }
            }

            items(virtualAccounts, key = { it.id }) { va ->
                GhostCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(maskAccount(va.accountNumber), style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${va.bankCode} - ${va.ifsc} - used ${va.uses}x",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            if (va.active) "ACTIVE" else "OFF",
                            if (va.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        TextButton(onClick = { onToggleVa(va.id, !va.active) }) { Text("flip") }
                        IconButton(onClick = { onRemoveVa(va.id) }) {
                            Icon(Icons.Default.Policy, contentDescription = "Remove virtual account")
                        }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Maintenance")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenTrace, modifier = Modifier.weight(1f)) {
                            Text("Trace audit")
                        }
                        OutlinedButton(onClick = onClearLogs, modifier = Modifier.weight(1f)) {
                            Text("Clear raw logs")
                        }
                    }
                    Text(
                        "Everything is on-device: no analytics, no cloud, no third-party SDK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
