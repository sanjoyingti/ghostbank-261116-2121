package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
                    SectionTitle("Proxy pool")
                    Text(
                        "One exit per line, http://host:port or socks5://host:port. Rotated per request; empty plus no Tor means the raw IP is used.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = proxyText,
                        onValueChange = { proxyText = it },
                        label = { Text("Exits") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                    Button(
                        onClick = {
                            vm.saveProxies(
                                proxyText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save exits") }
                    KeyValue("Exits saved", settings.proxyPool.size.toString())
                }
            }

            item {
                GhostCard {
                    SectionTitle("Harvest rules")
                    ToggleRow(
                        "Exact one paisa",
                        "Amount fixed at 0.01 with a hard cap; FLOOR only if the rail refuses",
                        settings.exactOnePaisa
                    ) { vm.setExactPaisa(it) }
                    ToggleRow(
                        "Cross-account fragmentation",
                        "One leg per account, spread over randomised gaps",
                        settings.fragmentation
                    ) { vm.setFragmentation(it) }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Raw log")
                    Text(
                        "Every exchange, with the full body kept so the next diagnosis needs no screenshots.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                        Text("Open trace audit")
                    }
                    OutlinedButton(onClick = { vm.clearLogs() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear raw log")
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Honest limits")
                    Text(
                        "The destination bank always knows your account number - it is in their own ledger. Proxy/Tor hides the IP, rotating virtual accounts and frozen per-account fingerprints break the repeating pattern, and nothing leaves the device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    KeyValue("Telemetry", "none")
                    KeyValue("App header", "none (Chrome UA only)")
                    KeyValue("Storage", "on-device Room database")
                }
            }
        }
    }
}
