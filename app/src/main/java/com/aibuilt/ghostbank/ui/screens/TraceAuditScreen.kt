package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.RegistryAudit
import com.aibuilt.ghostbank.data.db.HostLogEntity
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

@Composable
fun TraceAuditRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val logs by vm.hostLog.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val leak by vm.leak.collectAsStateWithLifecycle()

    TraceAuditScreen(
        logs = logs,
        settings = settings,
        auditReasons = leak.report?.reasons.orEmpty(),
        stealthScore = leak.report?.score,
        onBack = onBack,
        onClear = vm::clearLogs
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceAuditScreen(
    logs: List<HostLogEntity>,
    settings: GhostSettings,
    auditReasons: List<String>,
    stealthScore: Int?,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    var expandedId by remember { mutableStateOf<Long?>(null) }
    val violations = remember { RegistryAudit.violations() }

    Scaffold(topBar = { GhostTopBar(title = "Trace audit", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Registry self-check")
                    KeyValue("Banks", "${com.aibuilt.ghostbank.data.BankRegistry.banks.size}")
                    KeyValue("Verified endpoints", RegistryAudit.verifiedCount().toString())
                    KeyValue("Probe-only endpoints", RegistryAudit.probeOnlyCount().toString())
                    KeyValue(
                        "Invented debit route trusted",
                        if (RegistryAudit.bannedRouteInVerifiedSet()) "yes" else "no"
                    )
                    StatusPill(
                        if (violations.isEmpty()) "PASS - nothing verified without a live probe" else "FAIL",
                        if (violations.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    violations.forEach {
                        Text(
                            "${it.bankCode} ${it.endpointId}: ${it.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("What still leaked / what is hidden")
                    KeyValue("Exit", if (settings.torMode) "Tor" else "${settings.proxyPool.size} proxies")
                    KeyValue("DNS", if (settings.dohEnabled) "DoH inside tunnel" else "ISP resolver")
                    KeyValue("Cookies between runs", "none, client is ephemeral")
                    KeyValue("Destination on the wire", "rotating virtual account when configured")
                    KeyValue("Stealth score", stealthScore?.let { "$it / 100" } ?: "run the score on the Stealth screen")
                    auditReasons.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "Amber, physically unavoidable: the destination bank always knows your account number and the source bank knows the debited one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionTitle("Raw log (${logs.size})")
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            }

            if (logs.isEmpty()) {
                item {
                    GhostCard { Text("No exchanges logged yet.", style = MaterialTheme.typography.bodyMedium) }
                }
            }

            items(logs, key = { it.id }) { log ->
                GhostCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${log.method} ${log.stage}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                log.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "status ${log.status} - ${log.hops} hop(s) - ${log.elapsedMs}ms",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "final ${log.finalUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            when {
                                log.status in 200..299 -> "2xx"
                                log.status in 300..399 -> "3xx alive"
                                log.status < 0 -> "dead"
                                else -> "${log.status} dead"
                            },
                            when {
                                log.status in 200..399 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    TextButton(onClick = { expandedId = if (expandedId == log.id) null else log.id }) {
                        Text(if (expandedId == log.id) "Hide body" else "Show request + full body")
                    }
                    if (expandedId == log.id) {
                        Text("Request headers", style = MaterialTheme.typography.labelSmall)
                        MonoBlock(log.requestHeaders, maxHeight = 160)
                        Text("Response body", style = MaterialTheme.typography.labelSmall)
                        MonoBlock(
                            if (log.responseBody.isBlank()) "(empty)" else log.responseBody,
                            maxHeight = 320
                        )
                    }
                }
            }
        }
    }
}
