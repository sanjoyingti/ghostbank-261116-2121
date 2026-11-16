package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Policy
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
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.model.CanaryOutcome
import com.aibuilt.ghostbank.model.StealthLevel
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.ToggleRow
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.CanaryUi
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

    StealthScreen(
        settings = settings,
        canary = canary,
        onLevel = vm::setLevel,
        onTor = vm::setTor,
        onDoh = vm::setDoh,
        onKillSwitch = vm::setKillSwitch,
        onGeo = vm::setGeo,
        onDecoy = vm::setDecoy,
        onIdle = vm::setGhostIdle,
        onPadding = vm::setPadding,
        onDiurnal = vm::setDiurnal,
        onCanaryToggle = vm::setCanary,
        onRunCanary = vm::runCanary,
        onOpenLeak = onOpenLeak,
        onOpenTrace = onOpenTrace
    )
}

@Composable
fun StealthScreen(
    settings: GhostSettings,
    canary: CanaryUi,
    onLevel: (StealthLevel) -> Unit,
    onTor: (Boolean) -> Unit,
    onDoh: (Boolean) -> Unit,
    onKillSwitch: (Boolean) -> Unit,
    onGeo: (Boolean) -> Unit,
    onDecoy: (Boolean) -> Unit,
    onIdle: (Boolean) -> Unit,
    onPadding: (Boolean) -> Unit,
    onDiurnal: (Boolean) -> Unit,
    onCanaryToggle: (Boolean) -> Unit,
    onRunCanary: () -> Unit,
    onOpenLeak: () -> Unit,
    onOpenTrace: () -> Unit
) {
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
                                onClick = { onLevel(level) },
                                label = { Text(level.name) }
                            )
                        }
                    }
                    Text(
                        if (settings.stealthLevel == StealthLevel.GHOST) {
                            "Ghost is proxy-mandatory: nothing leaves on your raw IP."
                        } else {
                            "Higher levels add decoys, idling and stricter caps."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Transport")
                    ToggleRow(
                        title = "Tor mode",
                        subtitle = "Routes through Orbot's SOCKS5 listener on 127.0.0.1:9050",
                        checked = settings.torMode,
                        onChange = onTor
                    )
                    ToggleRow(
                        title = "DNS over HTTPS",
                        subtitle = "Keeps the ISP resolver from seeing bank hostnames",
                        checked = settings.dohEnabled,
                        onChange = onDoh
                    )
                    ToggleRow(
                        title = "Kill-switch",
                        subtitle = "Abandon any request that has no exit",
                        checked = settings.killSwitch,
                        onChange = onKillSwitch
                    )
                    ToggleRow(
                        title = "Geo-matched exit",
                        subtitle = "Prefer exits whose country matches the target banks",
                        checked = settings.geoMatch,
                        onChange = onGeo
                    )
                    KeyValue("Exits configured", settings.proxyPool.size.toString())
                }
            }

            item {
                GhostCard {
                    SectionTitle("Traffic shape")
                    ToggleRow(
                        title = "Decoy browsing",
                        subtitle = "Unrelated bank homepage loaded before each leg",
                        checked = settings.decoyBrowsing,
                        onChange = onDecoy
                    )
                    ToggleRow(
                        title = "Ghost idle",
                        subtitle = "A few idle keep-alives after settling, then a clean close",
                        checked = settings.ghostIdle,
                        onChange = onIdle
                    )
                    ToggleRow(
                        title = "Header padding",
                        subtitle = "Random padding so request sizes vary",
                        checked = settings.padding,
                        onChange = onPadding
                    )
                    ToggleRow(
                        title = "Diurnal window",
                        subtitle = "Slower outside ${settings.windowStartHour}:00-${settings.windowEndHour}:00",
                        checked = settings.diurnalWindow,
                        onChange = onDiurnal
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Detection watch")
                    ToggleRow(
                        title = "Canary gate",
                        subtitle = "Innocuous homepage GET first; a blocked canary stops the run",
                        checked = settings.canaryEnabled,
                        onChange = onCanaryToggle
                    )
                    Button(onClick = onRunCanary, enabled = !canary.running, modifier = Modifier.fillMaxWidth()) {
                        Text(if (canary.running) "Probing..." else "Run canary probe now")
                    }
                    canary.outcome?.let { outcome -> CanaryResult(outcome) }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Trace shield")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenLeak, modifier = Modifier.weight(1f)) {
                            Text("Leak check")
                        }
                        OutlinedButton(onClick = onOpenTrace, modifier = Modifier.weight(1f)) {
                            Text("Trace audit")
                        }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("What still leaks")
                    KeyValue("Your account number", "always known to your own bank")
                    KeyValue("Your IP", if (settings.exitConfigured) "hidden behind the exit" else "exposed - no exit")
                    KeyValue("Cookies between runs", "none, ephemeral sessions")
                    KeyValue("App signature", "none, no custom headers or telemetry")
                }
            }
        }
    }
}

@Composable
private fun CanaryResult(outcome: CanaryOutcome) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StatusPill(
            if (outcome.alive) "ALIVE" else "DEAD",
            if (outcome.alive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Text(
            "${outcome.detail} (hops ${outcome.hops})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
