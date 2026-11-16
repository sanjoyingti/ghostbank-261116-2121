package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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

    StealthScreen(
        vm = vm,
        level = settings.stealthLevel,
        tor = settings.torMode,
        doh = settings.dohEnabled,
        killSwitch = settings.killSwitch,
        diurnal = settings.diurnalWindow,
        decoy = settings.decoyBrowsing,
        idle = settings.ghostIdle,
        padding = settings.padding,
        canary = settings.canaryEnabled,
        fragmentation = settings.fragmentation,
        geoMatch = settings.geoMatch,
        rpm = settings.maxRequestsPerMinute,
        aph = settings.maxAccountsPerHour,
        proxyCount = settings.proxyPool.size,
        score = leak.report?.score,
        reasons = leak.report?.reasons.orEmpty(),
        onOpenLeak = onOpenLeak,
        onOpenTrace = onOpenTrace
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealthScreen(
    vm: SettingsViewModel,
    level: StealthLevel,
    tor: Boolean,
    doh: Boolean,
    killSwitch: Boolean,
    diurnal: Boolean,
    decoy: Boolean,
    idle: Boolean,
    padding: Boolean,
    canary: Boolean,
    fragmentation: Boolean,
    geoMatch: Boolean,
    rpm: Int,
    aph: Int,
    proxyCount: Int,
    score: Int?,
    reasons: List<String>,
    onOpenLeak: () -> Unit,
    onOpenTrace: () -> Unit
) {
    var rpmDraft by remember { mutableStateOf(rpm.toString()) }
    var aphDraft by remember { mutableStateOf(aph.toString()) }

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
                        StealthLevel.entries.forEach { option ->
                            FilterChip(
                                selected = option == level,
                                onClick = { vm.setLevel(option) },
                                label = { Text(option.name) }
                            )
                        }
                    }
                    Text(
                        when (level) {
                            StealthLevel.STANDARD -> "Headers only. Fast, most visible."
                            StealthLevel.DEEP -> "Fresh fingerprint, jitter, decoys, canary."
                            StealthLevel.GHOST -> "Proxy mandatory, one burned profile per account."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Transport")
                    KeyValue("Exits configured", proxyCount.toString())
                    KeyValue("Tor mode", if (tor) "on (127.0.0.1:9050)" else "off")
                    ToggleRow("Tor mode", "Route everything through Orbot", tor, vm::setTor)
                    ToggleRow("DNS-over-HTTPS", "Resolve names inside the tunnel", doh, vm::setDoh)
                    ToggleRow("Kill-switch", "Abandon any call without an exit", killSwitch, vm::setKillSwitch)
                    ToggleRow("Geo-matched exit", "Re-pick if the exit is not in India", geoMatch, vm::setGeo)
                    OutlinedButton(onClick = { vm.score() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Compute stealth score")
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Timing and cover traffic")
                    ToggleRow("Poisson jitter", "Cluster gaps like a human", true, {})
                    ToggleRow("Diurnal window", "Slow down at night", diurnal, vm::setDiurnal)
                    ToggleRow("Decoy browsing", "Load an unrelated bank first", decoy, vm::setDecoy)
                    ToggleRow("Ghost idle", "Idle keep-alives before closing", idle, vm::setGhostIdle)
                    ToggleRow("Padding", "Random pad header on every call", padding, vm::setPadding)
                    ToggleRow("Canary gate", "Innocuous GET before any run", canary, vm::setCanary)
                    ToggleRow("Amount fragmentation", "Split pulls across virtual accounts", fragmentation, vm::setFragmentation)
                }
            }

            item {
                GhostCard {
                    SectionTitle("Velocity caps")
                    OutlinedTextField(
                        value = rpmDraft,
                        onValueChange = { rpmDraft = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Max requests per minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = aphDraft,
                        onValueChange = { aphDraft = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Max accounts per hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            vm.setCaps(
                                rpmDraft.toIntOrNull() ?: rpm,
                                aphDraft.toIntOrNull() ?: aph
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save caps") }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Stealth score")
                    if (score == null) {
                        Text(
                            "Not computed yet. The score counts the switches that are actually on.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        StatusPill(
                            "score $score / 100",
                            if (score >= 70) MaterialTheme.colorScheme.primary
                            else if (score >= 45) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error
                        )
                        reasons.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenLeak, modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.Icon(Icons.Default.Public, contentDescription = null)
                        Text("  Leak check lab")
                    }
                    OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.Icon(Icons.Default.ReceiptLong, contentDescription = null)
                        Text("  Trace audit")
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Honest limit")
                    Text(
                        "Your account number is always in the bank's own ledger. What GhostBank hides is the link: masked virtual digits, a different exit per run, a frozen profile burned after one paisa and no app signature.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
