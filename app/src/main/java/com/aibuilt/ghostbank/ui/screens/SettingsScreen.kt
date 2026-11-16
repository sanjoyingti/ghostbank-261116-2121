package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.settings.GhostSettings
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
    val proxyText by vm.proxyText.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        proxyText = proxyText,
        onProxyText = vm::loadProxies,
        onSaveProxies = vm::saveProxies,
        onTor = vm::setTor,
        onDoh = vm::setDoh,
        onKillSwitch = vm::setKillSwitch,
        onExactPaisa = vm::setExactPaisa,
        onFragmentation = vm::setFragmentation,
        onCaps = vm::setCaps,
        onWindow = vm::setWindow,
        onOpenTrace = onOpenTrace
    )
}

@Composable
fun SettingsScreen(
    settings: GhostSettings,
    proxyText: String?,
    onProxyText: (String) -> Unit,
    onSaveProxies: (String) -> Unit,
    onTor: (Boolean) -> Unit,
    onDoh: (Boolean) -> Unit,
    onKillSwitch: (Boolean) -> Unit,
    onExactPaisa: (Boolean) -> Unit,
    onFragmentation: (Boolean) -> Unit,
    onCaps: (Int, Int) -> Unit,
    onWindow: (Int, Int) -> Unit,
    onOpenTrace: () -> Unit
) {
    val draft = proxyText ?: remember(settings.proxyPool) {
        settings.proxyPool.joinToString("\n") { it.raw }
    }
    var rpm by remember(settings.maxRequestsPerMinute) { mutableStateOf(settings.maxRequestsPerMinute.toString()) }
    var aph by remember(settings.maxAccountsPerHour) { mutableStateOf(settings.maxAccountsPerHour.toString()) }
    var start by remember(settings.windowStartHour) { mutableStateOf(settings.windowStartHour.toString()) }
    var end by remember(settings.windowEndHour) { mutableStateOf(settings.windowEndHour.toString()) }

    LaunchedEffect(Unit) { }

    Scaffold(topBar = { GhostTopBar(title = "Settings") }) { padding ->
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
                    SectionTitle("Exit")
                    Text(
                        "Proxy-mandatory when the dial says Ghost: no exit, no traffic. One entry per line, http://host:port or socks5://host:port.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = draft,
                        onValueChange = onProxyText,
                        label = { Text("Proxy pool") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onSaveProxies(draft) }) { Text("Save pool") }
                        TextButton(onClick = { onSaveProxies("") }) { Text("Clear") }
                    }
                    KeyValue("Exits", settings.proxyPool.size.toString())
                    ToggleRow("Tor mode", "SOCKS5 to Orbot on 127.0.0.1:9050", settings.torMode, onTor)
                    ToggleRow("DNS over HTTPS", "Resolve inside the tunnel", settings.dohEnabled, onDoh)
                    ToggleRow("Kill-switch", "Abandon the call instead of leaking the raw IP", settings.killSwitch, onKillSwitch)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount")
                    ToggleRow("Exact 1 paisa", "Hard-capped at 0.01 rupee, never drifts", settings.exactOnePaisa, onExactPaisa)
                    ToggleRow("Amount fragmentation", "Split legs across virtual accounts where possible", settings.fragmentation, onFragmentation)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Velocity")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rpm,
                            onValueChange = { rpm = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Requests / minute") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = aph,
                            onValueChange = { aph = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Accounts / hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            onCaps(rpm.toIntOrNull() ?: 60, aph.toIntOrNull() ?: 40)
                        }
                    ) { Text("Save caps") }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Diurnal window")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = start,
                            onValueChange = { start = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("From hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = end,
                            onValueChange = { end = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("To hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            onWindow(
                                (start.toIntOrNull() ?: 9).coerceIn(0, 23),
                                (end.toIntOrNull() ?: 23).coerceIn(0, 23)
                            )
                        }
                    ) { Text("Save window") }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Diagnostics")
                    Text(
                        "Nothing leaves the device: no telemetry, no analytics, no cloud ledger.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onOpenTrace) { Text("Open trace audit") }
                }
            }

            item { Column { Text("GhostBank - local only", style = MaterialTheme.typography.bodySmall) } }
        }
    }
}
