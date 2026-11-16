package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    val report by vm.leak.collectAsStateWithLifecycle()

    StealthScreen(
        settings = settings,
        score = report.report?.score,
        reasons = report.report?.reasons.orEmpty(),
        onLevel = vm::setLevel,
        onTor = vm::setTor,
        onDoh = vm::setDoh,
        onKillSwitch = vm::setKillSwitch,
        onDiurnal = vm::setDiurnal,
        onWindow = vm::setWindow,
        onDecoy = vm::setDecoy,
        onIdle = vm::setGhostIdle,
        onFragment = vm::setFragmentation,
        onCanary = vm::setCanary,
        onPadding = vm::setPadding,
        onGeo = vm::setGeo,
        onCaps = vm::setCaps,
        onScore = vm::score,
        onOpenLeak = onOpenLeak,
        onOpenTrace = onOpenTrace
    )
}

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
    onWindow: (Int, Int) -> Unit,
    onDecoy: (Boolean) -> Unit,
    onIdle: (Boolean) -> Unit,
    onFragment: (Boolean) -> Unit,
    onCanary: (Boolean) -> Unit,
    onPadding: (Boolean) -> Unit,
    onGeo: (Boolean) -> Unit,
    onCaps: (Int, Int) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        "Ghost is proxy-mandatory: with no exit, the run refuses to start rather than leak the raw IP.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(
                            if (settings.exitConfigured) "exit configured" else "no exit",
                            if (settings.exitConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (settings.torMode) "tor" else "${settings.proxyPool.size} proxies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Transport")
                    ToggleRow("Tor mode", "Route through a local SOCKS 9050 listener", settings.torMode, onTor)
                    ToggleRow("DNS-over-HTTPS", "Names resolve inside the tunnel, ISP resolver bypassed", settings.dohEnabled, onDoh)
                    ToggleRow("Kill-switch", "Abandon the request if the exit drops, never send raw", settings.killSwitch, onKillSwitch)
                    ToggleRow("Geo-matched exit", "Re-pick an exit whose country matches the banks", settings.geoMatch, onGeo)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing")
                    ToggleRow(
                        "Diurnal window",
                        "Slow down at night, ${settings.windowStartHour}:00 to ${settings.windowEndHour}:00",
                        settings.diurnalWindow,
                        onDiurnal
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onWindow((settings.windowStartHour + 1) % 24, settings.windowEndHour) }) {
                            Text("start ${settings.windowStartHour}h")
                        }
                        OutlinedButton(onClick = { onWindow(settings.windowStartHour, (settings.windowEndHour + 1) % 24) }) {
                            Text("end ${settings.windowEndHour}h")
                        }
                    }
                    Text(
                        "Gaps use a Poisson distribution, so they cluster like a person instead of ticking like a cron job.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Velocity caps")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = settings.maxRequestsPerMinute.toString(),
                            onValueChange = { onCaps(it.toIntOrNull() ?: settings.maxRequestsPerMinute, settings.maxAccountsPerHour) },
                            label = { Text("req/min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = settings.maxAccountsPerHour.toString(),
                            onValueChange = { onCaps(settings.maxRequestsPerMinute, it.toIntOrNull() ?: settings.maxAccountsPerHour) },
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
                    SectionTitle("Cover traffic & shape")
                    ToggleRow("Decoy browsing", "Load an unrelated bank first so a burst is not only the endpoint", settings.decoyBrowsing, onDecoy)
                    ToggleRow("Ghost idle", "Idle keep-alives after settling, then a clean close", settings.ghostIdle, onIdle)
                    ToggleRow("Amount fragmentation", "Split pulls across minutes where the amount allows", settings.fragmentation, onFragment)
                    ToggleRow("Padding & encoding variation", "Random pad header and encoding negotiation", settings.padding, onPadding)
                    ToggleRow("Canary gate", "Innocuous GET first, run never starts if blocked", settings.canaryEnabled, onCanary)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth score")
                    if (score == null) {
                        Text(
                            "Not computed yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("$score / 100", style = MaterialTheme.typography.headlineSmall)
                    }
                    Button(onClick = onScore, modifier = Modifier.fillMaxWidth()) { Text("Score current settings") }
                    reasons.forEach {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Button(onClick = onOpenLeak, modifier = Modifier.fillMaxWidth()) { Text("Open Leak Lab") }
            }
            item {
                OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Trace Audit")
                }
            }
        }
    }
}
