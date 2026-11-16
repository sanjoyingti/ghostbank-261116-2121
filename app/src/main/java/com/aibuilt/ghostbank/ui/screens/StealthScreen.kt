package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
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
fun StealthRoute(
    container: AppContainer,
    onOpenLeak: () -> Unit,
    onOpenTrace: () -> Unit
) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val leak by vm.leak.collectAsStateWithLifecycle()

    Scaffold(topBar = { GhostTopBar(title = "Stealth") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GhostCard {
                SectionTitle("Stealth level")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StealthLevel.entries.forEach { level ->
                        FilterChip(
                            selected = settings.stealthLevel == level,
                            onClick = { vm.setLevel(level) },
                            label = { Text(level.name) }
                        )
                    }
                }
                Text(
                    "Ghost forces a proxy or Tor, because the Android network stack can only be hidden behind an exit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                KeyValue("Score", leak.report?.score?.toString() ?: "-")
                OutlinedButton(onClick = vm::score, modifier = Modifier.fillMaxWidth()) {
                    Text("Recalculate stealth score")
                }
            }

            GhostCard {
                SectionTitle("Exit")
                ToggleRow("Tor mode", "Route everything through Orbot on 127.0.0.1:9050", settings.torMode, vm::setTor)
                ToggleRow("Geo-matched exit", "Re-pick the exit when its country does not match", settings.geoMatch, vm::setGeo)
                KeyValue("Proxy pool", "${settings.proxyPool.size} entries")
            }

            GhostCard {
                SectionTitle("Cover traffic")
                ToggleRow("Decoy browsing", "Load an unrelated bank first", settings.decoyBrowsing, vm::setDecoy)
                ToggleRow("Ghost idle", "Idle keep-alive junks after settling", settings.ghostIdle, vm::setGhostIdle)
                ToggleRow("Padding", "Random padding header and encoding jitter", settings.padding, vm::setPadding)
                ToggleRow("Fragmentation", "Spread legs across randomised gaps", settings.fragmentation, vm::setFragmentation)
                ToggleRow("Diurnal window", "Slow down at night", settings.diurnalWindow, vm::setDiurnal)
                KeyValue("Window", "${settings.windowStartHour}:00 - ${settings.windowEndHour}:00")
            }

            GhostCard {
                SectionTitle("Detection reflexes")
                ToggleRow("Canary gate", "Innocuous GET first, abort when blocked", settings.canaryEnabled, vm::setCanary)
                ToggleRow("DNS-over-HTTPS", "Resolve through the tunnel", settings.dohEnabled, vm::setDoh)
                ToggleRow("Kill-switch", "Never send on the raw IP", settings.killSwitch, vm::setKillSwitch)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        text = "max ${settings.maxRequestsPerMinute}/min",
                        color = MaterialTheme.colorScheme.secondary
                    )
                    StatusPill(
                        text = "max ${settings.maxAccountsPerHour}/hour",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            GhostCard {
                SectionTitle("Verify before you run")
                Button(onClick = onOpenLeak, modifier = Modifier.fillMaxWidth()) { Text("Open leak check") }
                OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                    Text("Open trace audit")
                }
                OutlinedButton(onClick = vm::runCanary, modifier = Modifier.fillMaxWidth()) {
                    Text("Run canary probe now")
                }
                vm.canary.collectAsStateWithLifecycle().value.outcome?.let { outcome ->
                    Text(
                        if (outcome.alive) "Canary ALIVE - ${outcome.detail}" else "Canary DEAD - ${outcome.detail}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (outcome.alive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}
