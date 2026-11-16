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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
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
    val logs by vm.hostLog.collectAsStateWithLifecycle()
    val canary by vm.canary.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            GhostTopBar(title = "Trace audit", onBack = onBack, actions = {
                IconButton(onClick = { vm.clearLogs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                }
            })
        }
    ) { padding ->
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
                    SectionTitle("Canary probe")
                    Text(
                        "2xx and 3xx count as alive. Only 4xx and 5xx count as dead.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = vm::runCanary, enabled = !canary.running, modifier = Modifier.fillMaxWidth()) {
                        Text("Probe now")
                    }
                    if (canary.running) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                            Text("  probing...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    canary.outcome?.let { outcome ->
                        StatusPill(
                            if (outcome.alive) "ALIVE" else "DEAD",
                            if (outcome.alive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        KeyValue("Status", outcome.status?.toString() ?: "none")
                        KeyValue("Hops", outcome.hops.toString())
                        KeyValue("Final URL", outcome.finalUrl.ifBlank { "-" })
                        Text(outcome.detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { SectionTitle("Exchanges (${logs.size})") }

            if (logs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Radar,
                        title = "No exchanges yet",
                        subtitle = "Every request is recorded here with its final address."
                    )
                }
            }

            items(logs, key = { it.id }) { log ->
                GhostCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(
                                "${log.status}",
                                if (log.status in 200..399) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            StatusPill(log.stage, MaterialTheme.colorScheme.secondary)
                            StatusPill("hops ${log.hops}", MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            "${log.method} ${log.url}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "final: ${log.finalUrl.ifBlank { "-" }}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${log.elapsedMs} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MonoBlock(log.responseBody.take(3000), maxHeight = 160)
                        Button(
                            onClick = {
                                clipboard.setText(
                                    AnnotatedString(
                                        "${log.method} ${log.url}\nfinal=${log.finalUrl}\nstatus=${log.status}\n" +
                                            log.responseBody
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Copy full body") }
                    }
                }
            }
        }
    }
}
