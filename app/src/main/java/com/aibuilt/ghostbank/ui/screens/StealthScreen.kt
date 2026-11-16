package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val canary by vm.canary.collectAsStateWithLifecycle()

    Scaffold(topBar = { GhostTopBar(title = "Stealth") }) { padding ->
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
                        when (settings.stealthLevel) {
                            StealthLevel.STANDARD -> "Standard: proxy optional, one identity per account, jitter on."
                            StealthLevel.DEEP -> "Deep: proxy pool rotated, decoy browsing, ghost idle, padding."
                            StealthLevel.GHOST -> "Ghost: proxy mandatory, geo-matched exit, canary gate, full entropy budget."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Transport")
                    ToggleRow("Tor mode", "Send everything through Orbot on 127.0.0.1:9050", settings.torMode, vm::setTor)
                    ToggleRow("DNS-over-HTTPS", "Resolve names inside the tunnel", settings.dohEnabled, vm::setDoh)
                    ToggleRow("Kill-switch", "Never send on the raw IP", settings.killSwitch, vm::setKillSwitch)
                    ToggleRow("Geo-matched exit", "Re-pick an exit whose country fits", settings.geoMatch, vm::setGeo)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing and velocity")
                    ToggleRow("Diurnal window", "Slower at night, faster in banking hours", settings.diurnalWindow, vm::setDiurnal)
                    KeyValue("Window", "${settings.windowStartHour}:00 to ${settings.windowEndHour}:00")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.setWindow((settings.windowStartHour - 1).coerceIn(0, 23), settings.windowEndHour) }) { Text("Start -1h") }
                        OutlinedButton(onClick = { vm.setWindow((settings.windowStartHour + 1).coerceIn(0, 23), settings.windowEndHour) }) { Text("Start +1h") }
                        OutlinedButton(onClick = { vm.setWindow(settings.windowStartHour, (settings.windowEndHour - 1).coerceIn(1, 23)) }) { Text("End -1h") }
                        OutlinedButton(onClick = { vm.setWindow(settings.windowStartHour, (settings.windowEndHour + 1).coerceIn(1, 24)) }) { Text("End +1h") }
                    }
                    KeyValue("Max requests / minute", settings.maxRequestsPerMinute.toString())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.setCaps(settings.maxRequestsPerMinute - 10, settings.maxAccountsPerHour) }) { Text("-10 rpm") }
                        OutlinedButton(onClick = { vm.setCaps(settings.maxRequestsPerMinute + 10, settings.maxAccountsPerHour) }) { Text("+10 rpm") }
                    }
                    KeyValue("Max accounts / hour", settings.maxAccountsPerHour.toString())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.setCaps(settings.maxRequestsPerMinute, settings.maxAccountsPerHour - 5) }) { Text("-5 aph") }
                        OutlinedButton(onClick = { vm.setCaps(settings.maxRequestsPerMinute, settings.maxAccountsPerHour + 5) }) { Text("+5 aph") }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Cover traffic and shape")
                    ToggleRow("Decoy browsing", "Load an unrelated bank first", settings.decoyBrowsing, vm::setDecoy)
                    ToggleRow("Ghost idle keep-alives", "Idle pings after a settle", settings.ghostIdle, vm::setGhostIdle)
                    ToggleRow("Amount fragmentation", "Split bigger pulls across legs", settings.fragmentation, vm::setFragmentation)
                    ToggleRow("Padding", "Randomised body and header padding", settings.padding, vm::setPadding)
                    ToggleRow("Canary gate", "Innocuous GET before any run", settings.canaryEnabled, vm::setCanary)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Canary and audit")
                    Button(onClick = vm::runCanary, enabled = !canary.running) { Text("Run canary probe") }
                    canary.outcome?.let { outcome ->
                        StatusPill(
                            if (outcome.alive) "ALIVE" else "DEAD",
                            if (outcome.alive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(outcome.detail, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onOpenLeak) { Text("Open Leak lab") }
                    TextButton(onClick = onOpenTrace) { Text("Open Trace audit") }
                }
            }

            item {
                Text(
                    "Honest limit: the TCP/IP stack fingerprint is Android's own. Only a proxy or Tor can shadow it - that is why Ghost level refuses to send without an exit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
