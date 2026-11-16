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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.model.StealthLevel
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.ToggleRow
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

@Composable
fun SettingsRoute(container: AppContainer, onOpenTrace: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        onLevel = vm::setLevel,
        onTor = vm::setTor,
        onDoh = vm::setDoh,
        onKillSwitch = vm::setKillSwitch,
        onDiurnal = vm::setDiurnal,
        onWindow = vm::setWindow,
        onCaps = vm::setCaps,
        onCanary = vm::setCanary,
        onExactPaisa = vm::setExactPaisa,
        onProxies = vm::saveProxies,
        onOpenTrace = onOpenTrace
    )
}

@Composable
fun SettingsScreen(
    settings: GhostSettings,
    onLevel: (StealthLevel) -> Unit,
    onTor: (Boolean) -> Unit,
    onDoh: (Boolean) -> Unit,
    onKillSwitch: (Boolean) -> Unit,
    onDiurnal: (Boolean) -> Unit,
    onWindow: (Int, Int) -> Unit,
    onCaps: (Int, Int) -> Unit,
    onCanary: (Boolean) -> Unit,
    onExactPaisa: (Boolean) -> Unit,
    onProxies: (List<String>) -> Unit,
    onOpenTrace: () -> Unit
) {
    var proxyText by remember(settings.proxyPool) {
        mutableStateOf(settings.proxyPool.joinToString("\n") { it.raw })
    }
    var rpm by remember(settings.maxRequestsPerMinute) {
        mutableFloatStateOf(settings.maxRequestsPerMinute.toFloat())
    }
    var aph by remember(settings.maxAccountsPerHour) {
        mutableFloatStateOf(settings.maxAccountsPerHour.toFloat())
    }
    var start by remember(settings.windowStartHour) {
        mutableFloatStateOf(settings.windowStartHour.toFloat())
    }
    var end by remember(settings.windowEndHour) {
        mutableFloatStateOf(settings.windowEndHour.toFloat())
    }

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
                    SectionTitle("Destination")
                    Text(
                        if (settings.destinationAccount.isBlank()) {
                            "No destination saved. Add an account and mark it as destination."
                        } else {
                            "${maskAccount(settings.destinationAccount)} · ${settings.destinationIfsc}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Proxy pool")
                    Text(
                        "One exit per line, http://host:port or socks5://host:port. Tor mode overrides the pool.",
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
                        Button(onClick = {
                            onProxies(
                                proxyText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                        }) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Text("  Save exits")
                        }
                        OutlinedButton(onClick = { proxyText = ""; onProxies(emptyList()) }) {
                            Text("Clear")
                        }
                    }
                    ToggleRow(
                        "Tor mode",
                        "Uses Orbot's SOCKS5 listener on 127.0.0.1:9050",
                        settings.torMode,
                        onTor
                    )
                    ToggleRow(
                        "Kill-switch",
                        "No exit, no traffic",
                        settings.killSwitch,
                        onKillSwitch
                    )
                    ToggleRow(
                        "DNS-over-HTTPS",
                        "Resolve names over HTTPS",
                        settings.dohEnabled,
                        onDoh
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth dial")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StealthLevel.entries.forEach { level ->
                            FilterChip(
                                selected = settings.stealthLevel == level,
                                onClick = { onLevel(level) },
                                label = { Text(level.name) }
                            )
                        }
                    }
                    ToggleRow(
                        "Canary gate",
                        "Probe first, pause if blocked",
                        settings.canaryEnabled,
                        onCanary
                    )
                    ToggleRow(
                        "Exact one paisa",
                        "Hard-lock the harvest amount to 1 paisa",
                        settings.exactOnePaisa,
                        onExactPaisa
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Velocity caps")
                    Text(
                        "Requests per minute: ${rpm.toInt()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = rpm,
                        onValueChange = { rpm = it },
                        onValueChangeFinished = { onCaps(rpm.toInt(), aph.toInt()) },
                        valueRange = 1f..300f
                    )
                    Text(
                        "Accounts per hour: ${aph.toInt()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = aph,
                        onValueChange = { aph = it },
                        onValueChangeFinished = { onCaps(rpm.toInt(), aph.toInt()) },
                        valueRange = 1f..500f
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Diurnal window")
                    ToggleRow(
                        "Shape timing by the clock",
                        "Waits stretch at night",
                        settings.diurnalWindow,
                        onDiurnal
                    )
                    Text("From ${start.toInt()}:00", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = start,
                        onValueChange = { start = it },
                        onValueChangeFinished = { onWindow(start.toInt(), end.toInt()) },
                        valueRange = 0f..23f
                    )
                    Text("To ${end.toInt()}:00", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = end,
                        onValueChange = { end = it },
                        onValueChangeFinished = { onWindow(start.toInt(), end.toInt()) },
                        valueRange = 1f..24f
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Diagnosis")
                    OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Timeline, contentDescription = null)
                        Text("  Open trace audit")
                    }
                    Text(
                        "No analytics, no cloud, no telemetry SDK. The ledger and the log stay on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
