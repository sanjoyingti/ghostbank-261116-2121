package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.model.StealthLevel
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

    SettingsScreen(
        settings = settings,
        onSaveProxies = vm::saveProxies,
        onTor = vm::setTor,
        onDoh = vm::setDoh,
        onKillSwitch = vm::setKillSwitch,
        onLevel = vm::setLevel,
        onDiurnal = vm::setDiurnal,
        onWindow = vm::setWindow,
        onCaps = vm::setCaps,
        onExactPaisa = vm::setExactPaisa,
        onDecoy = vm::setDecoy,
        onIdle = vm::setGhostIdle,
        onFragmentation = vm::setFragmentation,
        onCanary = vm::setCanary,
        onPadding = vm::setPadding,
        onOpenTrace = onOpenTrace,
        onClearLogs = vm::clearLogs
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: GhostSettings,
    onSaveProxies: (String) -> Unit,
    onTor: (Boolean) -> Unit,
    onDoh: (Boolean) -> Unit,
    onKillSwitch: (Boolean) -> Unit,
    onLevel: (StealthLevel) -> Unit,
    onDiurnal: (Boolean) -> Unit,
    onWindow: (Int, Int) -> Unit,
    onCaps: (Int, Int) -> Unit,
    onExactPaisa: (Boolean) -> Unit,
    onDecoy: (Boolean) -> Unit,
    onIdle: (Boolean) -> Unit,
    onFragmentation: (Boolean) -> Unit,
    onCanary: (Boolean) -> Unit,
    onPadding: (Boolean) -> Unit,
    onOpenTrace: () -> Unit,
    onClearLogs: () -> Unit
) {
    var proxyField by remember { mutableStateOf("") }
    var rpmField by remember { mutableStateOf(settings.maxRequestsPerMinute.toString()) }
    var aphField by remember { mutableStateOf(settings.maxAccountsPerHour.toString()) }

    LaunchedEffect(Unit) {
        proxyField = settings.proxyPool.joinToString("\n") { it.raw }
    }
    LaunchedEffect(settings.maxRequestsPerMinute) {
        rpmField = settings.maxRequestsPerMinute.toString()
    }
    LaunchedEffect(settings.maxAccountsPerHour) {
        aphField = settings.maxAccountsPerHour.toString()
    }

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
                    SectionTitle("Exit")
                    Text(
                        "Proxy or Tor first. With the kill-switch on and no exit, GhostBank refuses to send anything on the raw IP.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ToggleRow("Tor mode", "Orbot SOCKS5 on 127.0.0.1:9050", settings.torMode, onTor)
                    ToggleRow("Kill-switch", "Abandon the request if the exit drops", settings.killSwitch, onKillSwitch)
                    ToggleRow("DNS-over-HTTPS", "No ISP DNS leak", settings.dohEnabled, onDoh)
                    StatusPill(
                        if (settings.exitConfigured) "exit configured" else "no exit - traffic held",
                        if (settings.exitConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Proxy pool (one per line: host:port or socks5://host:port)")
                    OutlinedTextField(
                        value = proxyField,
                        onValueChange = { proxyField = it },
                        label = { Text("Exits") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSaveProxies(proxyField) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Save exits") }
                        OutlinedButton(
                            onClick = {
                                proxyField = ""
                                onSaveProxies("")
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Clear") }
                    }
                    KeyValue("Saved", "${settings.proxyPool.size} exits")
                }
            }

            item {
                GhostCard {
                    SectionTitle("Destination")
                    if (settings.destinationAccount.isBlank()) {
                        Text(
                            "Save an account as destination on the Account screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        KeyValue("Account", settings.destinationAccount)
                        KeyValue("IFSC", settings.destinationIfsc)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount rules")
                    ToggleRow("Exact 1 paisa", "Fixed at 0.01, hard capped", settings.exactOnePaisa, onExactPaisa)
                    Text(
                        "If the rail rejects sub-rupee the leg moves to the 100 paise floor and is tagged FLOOR. Cap is per bank and cannot be exceeded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth level")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StealthLevel.entries.forEach { level ->
                            OutlinedButton(onClick = { onLevel(level) }, modifier = Modifier.weight(1f)) {
                                Text(
                                    level.name,
                                    color = if (settings.stealthLevel == level) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing window")
                    ToggleRow("Diurnal window", "Behave by the clock, slow at night", settings.diurnalWindow, onDiurnal)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyValue("Start", "${settings.windowStartHour}:00")
                        TextButton(onClick = { onWindow((settings.windowStartHour + 23) % 24, settings.windowEndHour) }) {
                            Text("-")
                        }
                        TextButton(onClick = { onWindow((settings.windowStartHour + 1) % 24, settings.windowEndHour) }) {
                            Text("+")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyValue("End", "${settings.windowEndHour}:00")
                        TextButton(onClick = { onWindow(settings.windowStartHour, (settings.windowEndHour + 23) % 24) }) {
                            Text("-")
                        }
                        TextButton(onClick = { onWindow(settings.windowStartHour, (settings.windowEndHour + 1) % 24) }) {
                            Text("+")
                        }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Velocity caps")
                    OutlinedTextField(
                        value = rpmField,
                        onValueChange = { rpmField = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Max requests per minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = aphField,
                        onValueChange = { aphField = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Max accounts per hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onCaps(rpmField.toIntOrNull() ?: 60, aphField.toIntOrNull() ?: 40)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Apply caps") }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Cover traffic")
                    ToggleRow("Decoy browsing", "Load another bank's page first", settings.decoyBrowsing, onDecoy)
                    ToggleRow("Ghost idle keep-alives", "Idle pings then a clean close", settings.ghostIdle, onIdle)
                    ToggleRow("Padding", "Randomise padding", settings.padding, onPadding)
                    ToggleRow("Amount fragmentation", "Spread legs over virtual accounts", settings.fragmentation, onFragmentation)
                    ToggleRow("Canary auto-pause", "Blocked canary stops the run", settings.canaryEnabled, onCanary)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Maintenance")
                    OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                        Text("Open trace audit")
                    }
                    OutlinedButton(onClick = onClearLogs, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear raw log")
                    }
                    Text(
                        "Everything is on the device: encrypted local ledger, no telemetry, no third-party SDK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
