package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.BankRegistry
import com.aibuilt.ghostbank.data.db.HostLogEntity
import com.aibuilt.ghostbank.model.BankEndpoint
import com.aibuilt.ghostbank.ui.components.EmptyState
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
    val log by vm.hostLog.collectAsStateWithLifecycle()
    TraceAuditScreen(
        log = log,
        verified = BankRegistry.verifiedDebitEndpoints,
        probeOnly = BankRegistry.probeOnlyEndpoints,
        onClear = vm::clearLogs,
        onBack = onBack
    )
}

@Composable
fun TraceAuditScreen(
    log: List<HostLogEntity>,
    verified: List<BankEndpoint>,
    probeOnly: List<BankEndpoint>,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            GhostTopBar(title = "Trace audit", onBack = onBack, actions = {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear log")
                }
            })
        }
    ) { padding ->
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
                    SectionTitle("Endpoint registry")
                    Text(
                        "Only VERIFIED routes may carry a debit leg. UNVERIFIED ones are probe only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    KeyValue("Verified", verified.count { it.probeStatus != null }.toString())
                    KeyValue("Probe only", probeOnly.size.toString())
                }
            }

            items(probeOnly) { endpoint ->
                GhostCard {
                    Text(endpoint.id, style = MaterialTheme.typography.titleMedium)
                    Text(
                        endpoint.urlTemplate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        endpoint.probeEvidence,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    StatusPill("UNVERIFIED ${endpoint.probeStatus ?: ""}", MaterialTheme.colorScheme.tertiary)
                }
            }

            item { SectionTitle("Raw exchanges (${log.size})") }

            if (log.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "No exchanges yet",
                        subtitle = "Every request is written here, original status and final URL included."
                    )
                }
            } else {
                items(log, key = { it.id }) { entry ->
                    GhostCard {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${entry.method} ${entry.stage}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    entry.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(
                                "status ${entry.status}",
                                if (entry.status in 200..399) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        KeyValue("Hops", entry.hops.toString())
                        KeyValue("Final URL", entry.finalUrl)
                        KeyValue("Elapsed", "${entry.elapsedMs} ms")
                        MonoBlock(entry.requestHeaders, maxHeight = 120)
                        MonoBlock(entry.responseBody, maxHeight = 260)
                    }
                }
            }
        }
    }
}
