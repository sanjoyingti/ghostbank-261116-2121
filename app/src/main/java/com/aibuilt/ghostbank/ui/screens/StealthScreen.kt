package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.model.StealthLevel
import com.aibuilt.ghostbank.model.StealthReport
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.SectionTitle
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

    LaunchedEffect(Unit) { vm.score() }

    StealthScreen(
        settings = settings,
        report = leak.report,
        onLevel = vm::setLevel,
        onTor = vm::setTor,
        onDoh = vm::setDoh,
        onKillSwitch = vm::setKillSwitch,
        onGeo = vm::setGeo,
        onDecoy = vm::setDecoy,
        onIdle = vm::setGhostIdle,
        onPadding = vm::setPadding,
        onDiurnal = vm::setDiurnal,
        onCanary = vm::setCanary,
        onRescore = vm::score,
        onOpenLeak = onOpenLeak,
        onOpenTrace = onOpenTrace
    )
}

@Composable
fun StealthScreen(
    settings: GhostSettings,
    report: StealthReport?,
    onLevel: (StealthLevel) -> Unit,
    onTor: (Boolean) -> Unit,
    onDoh: (Boolean) -> Unit,
    onKillSwitch: (Boolean) -> Unit,
    onGeo: (Boolean) -> Unit,
    onDecoy: (Boolean) -> Unit,
    onIdle: (Boolean) -> Unit,
    onPadding: (Boolean) -> Unit,
    onDiurnal: (Boolean) -> Unit,
    onCanary: (Boolean) -> Unit,
    onRescore: () -> Unit,
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
                        "Ghost needs an exit configured. Every account gets its own frozen ghost profile, burned after its single paisa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth score")
                    val score = report?.score ?: 0
                    Text("$score / 100", style = MaterialTheme.typography.headlineSmall)
                    LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    (report?.reasons ?: emptyList()).forEach {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onRescore) { Text("Recompute score") }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Transport")
                    ToggleRow("Tor mode", "Route through Orbot's SOCKS5 listener on port 9050", settings.torMode, onTor)
                    ToggleRow("DNS over HTTPS", "Resolve names inside the tunnel so the ISP sees nothing", settings.dohEnabled, onDoh)
                    ToggleRow("Kill-switch", "Never send a call on the raw IP", settings.killSwitch, onKillSwitch)
                    ToggleRow("Geo-matched exit", "Re-pick an exit whose country matches the target", settings.geoMatch, onGeo)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Behaviour")
                    ToggleRow("Decoy browsing", "Load another bank's homepage before each leg", settings.decoyBrowsing, onDecoy)
                    ToggleRow("Ghost idle", "A few idle keep-alives after settling, then a clean close", settings.ghostIdle, onIdle)
                    ToggleRow("Traffic padding", "Random padding so bodies never share a size", settings.padding, onPadding)
                    ToggleRow("Diurnal window", "Slow down outside 09:00-23:00", settings.diurnalWindow, onDiurnal)
                    ToggleRow("Canary gate", "Innocuous GET first, run holds if it is blocked", settings.canaryEnabled, onCanary)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenLeak, modifier = Modifier.weight(1f)) {
                        Text("Leak check")
                    }
                    OutlinedButton(onClick = onOpenTrace, modifier = Modifier.weight(1f)) {
                        Text("Trace audit")
                    }
                }
            }

            item {
                Column {
                    Text(
                        "Honest limits: the destination bank always knows the destination account number, and Android's TCP stack is only hidden by a proxy or Tor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
