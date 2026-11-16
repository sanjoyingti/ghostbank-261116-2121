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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
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
import com.aibuilt.ghostbank.data.RegistryAudit
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
fun StealthRoute(container: AppContainer, onOpenLeak: () -> Unit, onOpenTrace: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val leak by vm.leak.collectAsStateWithLifecycle()
    val canary by vm.canary.collectAsStateWithLifecycle()

    Scaffold(topBar = { GhostTopBar(title = "Stealth", actions = {
        androidx.compose.material3.IconButton(onClick = onOpenTrace) {
            androidx.compose.material3.Icon(Icons.Default.BugReport, contentDescription = "Trace audit")
        }
    }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Stealth dial")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vm.levels.forEach { level ->
                            FilterChip(
                                selected = settings.stealthLevel == level,
                                onClick = { vm.setLevel(level) },
                                label = { Text(level.name) }
                            )
                        }
                    }
                    Text(
                        "Ghost dial refuses to send without a proxy or Tor. One frozen identity per account, burned after use.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Exit and leak control")
                    ToggleRow("Tor mode", "Route through Orbot on 127.0.0.1:9050", settings.torMode) { vm.setTor(it) }
                    ToggleRow("DNS over HTTPS", "Resolve names over HTTPS inside the tunnel", settings.dohEnabled) { vm.setDoh(it) }
                    ToggleRow("Kill-switch", "Abandon a call rather than use the raw IP", settings.killSwitch) { vm.setKillSwitch(it) }
                    ToggleRow("Geo-matched exit", "Prefer an Indian exit before a foreign one", settings.geoMatch) { vm.setGeo(it) }
                    KeyValue("Proxy pool", "${settings.proxyPool.size} exit(s) configured")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenLeak) { Text("Leak check") }
                        OutlinedButton(onClick = vm::runCanary, enabled = !canary.running) {
                            Text(if (canary.running) "Probing..." else "Canary probe")
                        }
                    }
                    canary.outcome?.let { outcome ->
                        val colour = if (outcome.alive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        StatusPill(if (outcome.alive) "ALIVE" else "BLOCKED", colour)
                        Text(outcome.detail, style = MaterialTheme.typography.bodySmall, color = colour)
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing and cover traffic")
                    ToggleRow("Poisson jitter", "Cluster the gaps like a human, not a cron", true) { }
                    ToggleRow("Diurnal window", "${settings.windowStartHour}:00 to ${settings.windowEndHour}:00, slower at night", settings.diurnalWindow) { vm.setDiurnal(it) }
                    ToggleRow("Decoy browsing", "Open an unrelated bank first", settings.decoyBrowsing) { vm.setDecoy(it) }
                    ToggleRow("Ghost idle", "Idle keep-alives after settling", settings.ghostIdle) { vm.setGhostIdle(it) }
                    ToggleRow("Traffic padding", "Random padding header per request", settings.padding) { vm.setPadding(it) }
                    ToggleRow("Canary gate", "Innocuous GET before every leg", settings.canaryEnabled) { vm.setCanary(it) }
                    ToggleRow("Amount fragmentation", "Rotate virtual destinations across legs", settings.fragmentation) { vm.setFragmentation(it) }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth score")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = vm::score) { Text("Score now") }
                    }
                    val report = leak.report
                    if (report == null) {
                        Text(
                            "Not scored yet. Requests/min cap ${settings.maxRequestsPerMinute}, accounts/hour cap ${settings.maxAccountsPerHour}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Score ${report.score} / 100", style = MaterialTheme.typography.titleLarge)
                        LinearProgressIndicator(progress = { report.score / 100f }, modifier = Modifier.fillMaxWidth())
                        report.reasons.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Registry self-check")
                    val issues = RegistryAudit.violations()
                    if (issues.isEmpty()) {
                        StatusPill("clean", MaterialTheme.colorScheme.primary)
                        Text(
                            "No endpoint claims to be verified without a live 2xx/3xx probe behind it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        issues.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
fun LeakCheckRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val leak by vm.leak.collectAsStateWithLifecycle()

    Scaffold(topBar = { GhostTopBar(title = "Leak check", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("What the far end actually sees")
                    Text(
                        "Live probes against real IP-echo and TLS endpoints. Green is hidden, amber physically cannot be.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = vm::runLeakCheck, enabled = !leak.running) {
                        Text(if (leak.running) "Probing..." else "Run leak check")
                    }
                    if (leak.running) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            if (leak.findings.isEmpty() && !leak.running) {
                item {
                    GhostCard {
                        androidx.compose.material3.Icon(Icons.Default.Visibility, contentDescription = null)
                        Text(
                            "Nothing measured yet. Run the check before a harvest so you know the exit is real.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(leak.findings) { finding ->
                GhostCard {
                    Row {
                        StatusPill(
                            if (finding.hidden) "HIDDEN" else "VISIBLE",
                            if (finding.hidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(finding.label, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(finding.value, style = MaterialTheme.typography.bodySmall)
                    Text(finding.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TraceAuditRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val log by vm.hostLog.collectAsStateWithLifecycle()

    Scaffold(topBar = { GhostTopBar(title = "Trace audit", onBack = onBack, actions = {
        androidx.compose.material3.TextButton(onClick = vm::clearLogs) { Text("Clear") }
    }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Every exchange, with its verdict")
                    Text(
                        "Original status, hop count and the final URL after redirects are kept here, plus the whole response body so the next diagnosis needs no screenshot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    KeyValue("Logged exchanges", log.size.toString())
                }
            }
            if (log.isEmpty()) {
                item {
                    GhostCard {
                        androidx.compose.material3.Icon(Icons.Default.Shield, contentDescription = null)
                        Text("No traffic logged yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(log, key = { it.id }) { entry ->
                GhostCard {
                    Row {
                        StatusPill(
                            entry.status.toString(),
                            if (entry.status in 200..399) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(entry.stage, style = MaterialTheme.typography.bodyLarge)
                    }
                    KeyValue("Method", entry.method)
                    KeyValue("Requested", entry.url)
                    KeyValue("Final URL", entry.finalUrl)
                    KeyValue("Hops", entry.hops.toString())
                    KeyValue("Elapsed", "${entry.elapsedMs}ms")
                    Text("Request headers", style = MaterialTheme.typography.labelSmall)
                    MonoBlock(entry.requestHeaders, maxHeight = 120)
                    Text("Response body", style = MaterialTheme.typography.labelSmall)
                    MonoBlock(entry.responseBody.ifBlank { "(empty)" }, maxHeight = 220)
                }
            }
        }
    }
}
