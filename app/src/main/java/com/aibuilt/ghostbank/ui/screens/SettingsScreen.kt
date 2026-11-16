package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
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
        onKillSwitch = vm::setKillSwitch,
        onExactPaisa = vm::setExactPaisa,
        onCaps = vm::setCaps,
        onClearLogs = vm::clearLogs,
        onOpenTrace = onOpenTrace
    )
}

@Composable
fun SettingsScreen(
    settings: GhostSettings,
    onSaveProxies: (String) -> Unit,
    onTor: (Boolean) -> Unit,
    onKillSwitch: (Boolean) -> Unit,
    onExactPaisa: (Boolean) -> Unit,
    onCaps: (Int, Int) -> Unit,
    onClearLogs: () -> Unit,
    onOpenTrace: () -> Unit
) {
    var proxyText by remember { mutableStateOf(settings.proxyPool.joinToString("\n") { it.raw }) }
    LaunchedEffect(settings.proxyPool) {
        proxyText = settings.proxyPool.joinToString("\n") { it.raw }
    }

    Scaffold(topBar = { GhostTopBar(title = "Settings") }) { padding ->
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
                    KeyValue("Account", settings.destinationAccount.ifBlank { "not set" })
                    KeyValue("IFSC", settings.destinationIfsc.ifBlank { "not set" })
                    Text(
                        "Set it on the Account screen with the star. Credits fall back to the source IFSC while it is blank.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Exit")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(
                            if (settings.exitConfigured) "exit configured" else "no exit",
                            if (settings.exitConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tor listener 127.0.0.1:9050",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ToggleRow("Tor mode", "Use the local Tor SOCKS listener", settings.torMode, onTor)
                    ToggleRow("Kill-switch", "Hold traffic when no exit is configured", settings.killSwitch, onKillSwitch)
                    OutlinedTextField(
                        value = proxyText,
                        onValueChange = { proxyText = it },
                        label = { Text("Proxy pool, one per line") },
                        placeholder = { Text("socks5://host:1080\nhttp://user:pass@host:8080") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                    Button(onClick = { onSaveProxies(proxyText) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Save proxy pool")
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Amount")
                    ToggleRow(
                        "Exact 1-paisa mode",
                        "Hard cap at Rs 0.01; a refusal floors to 100 paise and is tagged FLOOR",
                        settings.exactOnePaisa,
                        onExactPaisa
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Caps")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = settings.maxRequestsPerMinute.toString(),
                            onValueChange = {
                                onCaps(it.toIntOrNull() ?: settings.maxRequestsPerMinute, settings.maxAccountsPerHour)
                            },
                            label = { Text("req/min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = settings.maxAccountsPerHour.toString(),
                            onValueChange = {
                                onCaps(settings.maxRequestsPerMinute, it.toIntOrNull() ?: settings.maxAccountsPerHour)
                            },
                            label = { Text("accounts/hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Data")
                    Text(
                        "Everything lives on the device: database, ledger and raw log. No analytics, no cloud, no third-party SDK.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onClearLogs, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear traffic log")
                    }
                    TextButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Trace Audit")
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Honest limits")
                    Text(
                        "1. The destination bank always knows the credited account number - it is their ledger.\n" +
                            "2. IMPS and UPI reject sub-rupee, so a 1-paisa leg either completes or floors to Rs 1 with a FLOOR tag.\n" +
                            "3. Only gateway routes proven by a live probe may carry a debit leg; invented paths stay probe-only.\n" +
                            "4. The TCP/IP stack is Android's own - only a proxy or Tor can shadow it, which is why Ghost mode is proxy-mandatory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "GhostBank 1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
