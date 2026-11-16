package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.ToggleRow
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

@Composable
fun StealthRoute(container: AppContainer, onOpenLeak: () -> Unit, onOpenTrace: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val leak by vm.leak.collectAsStateWithLifecycle()

    StealthScreen(
        settings = settings,
        score = leak.report?.score,
        reasons = leak.report?.reasons.orEmpty(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealthScreen(
    settings: GhostSettings,
    score: Int?,
    reasons: List<String>,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Stealth level")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StealthLevel.entries.forEach { level ->
                            OutlinedButton(
                                onClick = { onLevel(level) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    level.name,
                                    color = if (settings.stealthLevel == level) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        if (settings.stealthLevel == StealthLevel.GHOST) {
                            "Ghost: an exit is mandatory, no traffic ever leaves on the raw IP."
                        } else {
                            "Deep: full profile freeze, jitter and cover traffic. Standard: lighter shaping."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Exit")
                    ToggleRow("Tor mode", "Use Orbot SOCKS5 on 127.0.0.1:9050", settings.torMode, onTor)
                    ToggleRow("Kill-switch", "Abandon a request when the exit drops", settings.killSwitch, onKillSwitch)
                    ToggleRow("DNS-over-HTTPS", "Resolve names inside the tunnel", settings.dohEnabled, onDoh)
                    ToggleRow("Geo-matched exit", "Re-pick an exit whose geo does not match", settings.geoMatch, onGeo)
                    KeyValue("Proxy pool", "${settings.proxyPool.size} exits")
                    if (!settings.exitConfigured) {
                        StatusPill("no exit configured - nothing is sent", MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing")
                    ToggleRow("Diurnal window", "${settings.windowStartHour}:00 to ${settings.windowEndHour}:00, slowed at night", settings.diurnalWindow, onDiurnal)
                    KeyValue("Velocity cap", "${settings.maxRequestsPerMinute}/min, ${settings.maxAccountsPerHour}/hour")
                    Text(
                        "Gaps follow a Poisson distribution so they cluster like a person instead of a metronome.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Cover traffic and shape")
                    ToggleRow("Decoy browsing", "Load another bank's homepage before a leg", settings.decoyBrowsing, onDecoy)
                    ToggleRow("Ghost idle", "1-3 idle keep-alives, then a clean close", settings.ghostIdle, onIdle)
                    ToggleRow("Padding", "Random request padding header", settings.padding, onPadding)
                    ToggleRow("Amount fragmentation", "Split a pull across virtual accounts", settings.fragmentation, onFragmentation)
                    ToggleRow("Canary gate", "Blocked canary means the run never starts", settings.canaryEnabled, onCanary)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth score")
                    Text(
                        score?.let { "$it / 100" } ?: "not measured yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    reasons.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = onScore, modifier = Modifier.fillMaxWidth()) { Text("Score current settings") }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenLeak, modifier = Modifier.weight(1f)) { Text("Leak check") }
                    OutlinedButton(onClick = onOpenTrace, modifier = Modifier.weight(1f)) { Text("Trace audit") }
                }
            }

            item {
                Text(
                    "Honest limit: the TCP/IP stack is Android's own. Only a proxy or Tor shadows it, which is why Ghost is proxy-mandatory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
