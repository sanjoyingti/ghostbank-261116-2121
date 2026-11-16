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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlin.math.roundToInt

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
                        vm.levels.forEach { level ->
                            FilterChip(
                                selected = settings.stealthLevel == level,
                                onClick = { vm.setLevel(level) },
                                label = { Text(level.name) }
                            )
                        }
                    }
                    Text(
                        "Ghost refuses to send anything unless Tor or a proxy exit exists.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Transport")
                    ToggleRow("Tor mode", "Route everything through Orbot on 127.0.0.1:9050", settings.torMode) { vm.setTor(it) }
                    ToggleRow("DNS over HTTPS", "Names never reach the ISP resolver", settings.dohEnabled) { vm.setDoh(it) }
                    ToggleRow("Kill-switch", "Abandon a call rather than use the raw IP", settings.killSwitch) { vm.setKillSwitch(it) }
                    ToggleRow("Geo-matched exit", "Re-pick an exit whose country fits the bank", settings.geoMatch) { vm.setGeo(it) }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing and shape")
                    ToggleRow("Diurnal window", "Slow down outside the chosen hours", settings.diurnalWindow) { vm.setDiurnal(it) }
                    KeyValue("Window", "${settings.windowStartHour}:00 to ${settings.windowEndHour}:00")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { vm.setWindow((settings.windowStartHour + 1) % 24, settings.windowEndHour) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Start +1h") }
                        OutlinedButton(
                            onClick = { vm.setWindow(settings.windowStartHour, (settings.windowEndHour + 1) % 24) },
                            modifier = Modifier.weight(1f)
                        ) { Text("End +1h") }
                    }
                    ToggleRow("Poisson jitter", "Gaps cluster like a person, not a cron", true) { }
                    ToggleRow("Decoy browsing", "Load an unrelated bank first", settings.decoyBrowsing) { vm.setDecoy(it) }
                    ToggleRow("Ghost idle", "Idle keep-alives after settling", settings.ghostIdle) { vm.setGhostIdle(it) }
                    ToggleRow("Traffic padding", "Random pad header on each exchange", settings.padding) { vm.setPadding(it) }
                    ToggleRow("Canary gate", "Probe before a run, pause if blocked", settings.canaryEnabled) { vm.setCanary(it) }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Velocity caps")
                    Text("Requests per minute: ${settings.maxRequestsPerMinute}")
                    Slider(
                        value = settings.maxRequestsPerMinute.toFloat(),
                        onValueChange = { vm.setCaps(it.roundToInt(), settings.maxAccountsPerHour) },
                        valueRange = 5f..200f
                    )
                    Text("Accounts per hour: ${settings.maxAccountsPerHour}")
                    Slider(
                        value = settings.maxAccountsPerHour.toFloat(),
                        onValueChange = { vm.setCaps(settings.maxRequestsPerMinute, it.roundToInt()) },
                        valueRange = 1f..200f
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth score")
                    val report = leak.report
                    if (report == null) {
                        Text(
                            "Score the current configuration.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("${report.score}/100", style = MaterialTheme.typography.headlineSmall)
                        report.reasons.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = vm::score, modifier = Modifier.fillMaxWidth()) { Text("Score now") }
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
        }
    }
}
