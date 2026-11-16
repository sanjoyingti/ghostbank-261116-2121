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

    var proxyText by remember { mutableStateOf("") }
    var rpm by remember { mutableStateOf("60") }
    var aph by remember { mutableStateOf("40") }
    var windowStart by remember { mutableStateOf("9") }
    var windowEnd by remember { mutableStateOf("23") }

    LaunchedEffect(settings.proxyPool) {
        proxyText = settings.proxyPool.joinToString("\n") { it.raw }
    }
    LaunchedEffect(settings.maxRequestsPerMinute, settings.maxAccountsPerHour) {
        rpm = settings.maxRequestsPerMinute.toString()
        aph = settings.maxAccountsPerHour.toString()
    }
    LaunchedEffect(settings.windowStartHour, settings.windowEndHour) {
        windowStart = settings.windowStartHour.toString()
        windowEnd = settings.windowEndHour.toString()
    }

    Scaffold(topBar = { GhostTopBar(title = "Settings") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GhostCard {
                SectionTitle("Proxy pool")
                Text(
                    "One exit per line: http://host:port or socks5://host:port. Vary the lines so exits rotate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = proxyText,
                    onValueChange = { proxyText = it },
                    label = { Text("Proxy list") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        vm.saveProxies(
                            proxyText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save exits") }
                KeyValue("Configured", "${settings.proxyPool.size}")
                ToggleRow("Tor mode", "Use Orbot instead of the pool", settings.torMode, vm::setTor)
            }

            GhostCard {
                SectionTitle("Speed caps")
                OutlinedTextField(
                    value = rpm,
                    onValueChange = { rpm = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Requests per minute") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = aph,
                    onValueChange = { aph = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Accounts per hour") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        vm.setCaps(
                            rpm.toIntOrNull() ?: 60,
                            aph.toIntOrNull() ?: 40
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save caps") }
            }

            GhostCard {
                SectionTitle("Diurnal window")
                ToggleRow("Restrict to window", "Slower outside banking hours", settings.diurnalWindow, vm::setDiurnal)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = windowStart,
                        onValueChange = { windowStart = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("From hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = windowEnd,
                        onValueChange = { windowEnd = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("To hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        vm.setWindow(
                            (windowStart.toIntOrNull() ?: 9).coerceIn(0, 23),
                            (windowEnd.toIntOrNull() ?: 23).coerceIn(0, 23)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save window") }
            }

            GhostCard {
                SectionTitle("Privacy")
                ToggleRow("DNS-over-HTTPS", "Resolve inside the tunnel", settings.dohEnabled, vm::setDoh)
                ToggleRow("Kill-switch", "Refuse to send on the raw IP", settings.killSwitch, vm::setKillSwitch)
                ToggleRow("Padding", "Add traffic shape noise", settings.padding, vm::setPadding)
                ToggleRow("Geo-matched exit", "Reject a mismatched country", settings.geoMatch, vm::setGeo)
            }

            GhostCard {
                SectionTitle("Amount")
                ToggleRow(
                    "Exact one paisa",
                    "Hard cap at 1 paisa, floor tagged FLOOR",
                    settings.exactOnePaisa,
                    vm::setExactPaisa
                )
            }

            GhostCard {
                SectionTitle("Maintenance")
                OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                    Text("Open trace audit")
                }
                OutlinedButton(onClick = vm::clearLogs, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear raw traffic log")
                }
                Text(
                    "Everything is on this phone: encrypted ledger, no telemetry, no cloud SDK.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
