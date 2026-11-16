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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timeline
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
import com.aibuilt.ghostbank.model.StealthLevel
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
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

    StealthScreen(
        settings = settings,
        onLevel = vm::setLevel,
        onTor = vm::setTor,
        onDoh = vm::setDoh,
        onKillSwitch = vm::setKillSwitch,
        onDiurnal = vm::setDiurnal,
        onDecoy = vm::setDecoy,
        onIdle = vm::setGhostIdle,
        onFragmentation = vm::setFragmentation,
        onCanary = vm::setCanary,
        onPadding = vm::setPadding,
        onGeo = vm::setGeo,
        onScore = vm::score,
        onOpenLeak = onOpenLeak,
        onOpenTrace = onOpenTrace
    )
}

@Composable
fun StealthScreen(
    settings: GhostSettings,
    onLevel: (StealthLevel) -> Unit,
    onTor: (Boolean) -> Unit,
    onDoh: (Boolean) -> Unit,
    onKillSwitch: (Boolean) -> Unit,
    onDiurnal: (Boolean) -> Unit,
    onDecoy: (Boolean) -> Unit,
    onIdle: (Boolean) -> Unit,
    onFragmentation: (Boolean) -> Unit,
    onCanary: (Boolean) -> Unit,
    onPadding: (Boolean) -> Unit,
    onGeo: (Boolean) -> Unit,
    onScore: () -> Unit,
    onOpenLeak: () -> Unit,
    onOpenTrace: () -> Unit
) {
    Scaffold(topBar = { GhostTopBar(title = "Stealth") }) { padding ->
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
                    SectionTitle("Dial")
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
                        if (settings.exitConfigured) {
                            "An exit is configured, so the run can start."
                        } else {
                            "No exit configured. The kill-switch holds every request rather than leak the raw IP."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (settings.exitConfigured) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Tunnel")
                    ToggleRow(
                        "Tor mode",
                        "Routes every exchange through Orbot on port 9050",
                        settings.torMode,
                        onTor
                    )
                    ToggleRow(
                        "DNS-over-HTTPS",
                        "Names resolve over HTTPS so the ISP resolver never sees bank hosts",
                        settings.dohEnabled,
                        onDoh
                    )
                    ToggleRow(
                        "Kill-switch",
                        "Abandon rather than send on the raw IP when no exit answers",
                        settings.killSwitch,
                        onKillSwitch
                    )
                    ToggleRow(
                        "Geo-matched exit",
                        "Re-pick an exit whose country and timezone match the target banks",
                        settings.geoMatch,
                        onGeo
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing")
                    ToggleRow(
                        "Diurnal window",
                        "Slow at night, faster in banking hours (${settings.windowStartHour}:00-${settings.windowEndHour}:00)",
                        settings.diurnalWindow,
                        onDiurnal
                    )
                    ToggleRow(
                        "Poisson jitter",
                        "Always on: gaps cluster like a person instead of a metronome",
                        true,
                        {}
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Cover traffic")
                    ToggleRow(
                        "Decoy browsing",
                        "Load an unrelated bank page before a leg so the burst is not only the endpoint",
                        settings.decoyBrowsing,
                        onDecoy
                    )
                    ToggleRow(
                        "Ghost idle",
                        "1-3 junk keep-alives after settling, then a clean close",
                        settings.ghostIdle,
                        onIdle
                    )
                    ToggleRow(
                        "Amount fragmentation",
                        "Splits a pull into random legs across rotating destination handles",
                        settings.fragmentation,
                        onFragmentation
                    )
                    ToggleRow(
                        "Traffic padding",
                        "Random padding and encoding negotiation on every request",
                        settings.padding,
                        onPadding
                    )
                    ToggleRow(
                        "Canary gate",
                        "Innocuous GET first; a blocked canary pauses the run",
                        settings.canaryEnabled,
                        onCanary
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Honest limits")
                    StatusPill("amber is physical", MaterialTheme.colorScheme.tertiary)
                    Text(
                        "Your own bank always knows your account number - it is in their ledger. The phone's TCP/IP stack is Android's. What the app can do is mask the exit and one-use the identity, and it says so on the trace screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenLeak, modifier = Modifier.weight(1f)) {
                        androidx.compose.material3.Icon(Icons.Default.Science, contentDescription = null)
                        Text("  Leak lab")
                    }
                    OutlinedButton(onClick = onOpenTrace, modifier = Modifier.weight(1f)) {
                        androidx.compose.material3.Icon(Icons.Default.Timeline, contentDescription = null)
                        Text("  Trace audit")
                    }
                }
                OutlinedButton(onClick = onScore, modifier = Modifier.fillMaxWidth()) {
                    Text("Recalculate stealth score")
                }
            }
        }
    }
}
