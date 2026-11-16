package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.model.StealthLevel
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.ToggleRow
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

@Composable
fun StealthRoute(
    container: AppContainer,
    onOpenLeak: () -> Unit,
    onOpenTrace: () -> Unit
) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val canary by vm.canary.collectAsStateWithLifecycle()
    val leak by vm.leak.collectAsStateWithLifecycle()
    var proxyText by remember { mutableStateOf("") }

    LaunchedEffect(settings.proxyPool) {
        proxyText = settings.proxyPool.joinToString("\n") { it.raw }
    }

    Scaffold(topBar = { GhostTopBar(title = "Stealth") }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Stealth level")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        vm.levels.forEach { level ->
                            FilterChip(
                                selected = settings.stealthLevel == level,
                                onClick = { vm.setLevel(level) },
                                label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Text(
                        if (settings.stealthLevel == StealthLevel.GHOST)
                            "Ghost is proxy-mandatory: no exit means no traffic at all."
                        else
                            "Deep is the recommended default. Standard trades stealth for speed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Exit")
                    ToggleRow(
                        title = "Tor mode",
                        subtitle = "Route through Orbot on 127.0.0.1:9050",
                        checked = settings.torMode,
                        onChange = vm::setTor
                    )
                    OutlinedTextField(
                        value = proxyText,
                        onValueChange = { proxyText = it },
                        label = { Text("Proxy pool, one per line (http:// or socks5://)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            vm.saveProxies(proxyText.split("\n").map { it.trim() }.filter { it.isNotEmpty() })
                        }) { Text("Save pool") }
                        OutlinedButton(onClick = onOpenLeak) { Text("Leak check") }
                    }
                    StatusPill(
                        if (settings.exitConfigured) "exit configured" else "no exit: kill-switch holds traffic",
                        if (settings.exitConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Transport and cover")
                    ToggleRow("DNS-over-HTTPS", "Resolve names inside the tunnel", settings.dohEnabled, vm::setDoh)
                    ToggleRow("Kill-switch", "Never send on the raw IP", settings.killSwitch, vm::setKillSwitch)
                    ToggleRow("Geo-matched exit", "Re-pick an exit whose country fits the bank", settings.geoMatch, vm::setGeo)
                    ToggleRow("Padding", "Randomised header and body padding", settings.padding, vm::setPadding)
                    ToggleRow("Decoy browsing", "Real GETs on an unrelated bank first", settings.decoyBrowsing, vm::setDecoy)
                    ToggleRow("Ghost idle", "Keep-alive junks after settling", settings.ghostIdle, vm::setGhostIdle)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing")
                    ToggleRow("Diurnal window", "Slow down at night", settings.diurnalWindow, vm::setDiurnal)
                    KeyValue("Window", "${settings.windowStartHour}:00 - ${settings.windowEndHour}:00")
                    ToggleRow("Amount fragmentation", "Split across accounts and minutes", settings.fragmentation, vm::setFragmentation)
                    ToggleRow("Exact one paisa", "Hard cap the harvest at 1 paisa", settings.exactOnePaisa, vm::setExactPaisa)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Canary")
                    Text(
                        "An innocuous homepage GET first. Blocked means the run never starts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = vm::runCanary, enabled = !canary.running) { Text("Run canary") }
                        OutlinedButton(onClick = vm::score) { Text("Score the setup") }
                    }
                    canary.outcome?.let { outcome ->
                        StatusPill(
                            if (outcome.alive) "ALIVE" else "DEAD",
                            if (outcome.alive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        MonoBlock(outcome.detail, maxHeight = 120)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth score")
                    val report = leak.report
                    if (report == null) {
                        Text(
                            "Nothing scored yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        KeyValue("Score", "${report.score}/100")
                        MonoBlock(report.reasons.joinToString("\n"), maxHeight = 200)
                    }
                    OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                        Text("Open trace audit")
                    }
                }
            }

            item {
                Text(
                    "No telemetry, no analytics SDK, no custom app header. Everything stays on device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
