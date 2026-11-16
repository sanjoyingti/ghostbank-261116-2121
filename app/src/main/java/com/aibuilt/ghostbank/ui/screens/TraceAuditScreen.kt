package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

@Composable
fun TraceAuditRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val log by vm.hostLog.collectAsStateWithLifecycle()

    TraceAuditScreen(log = log, onClear = { vm.clearLogs() }, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceAuditScreen(
    log: List<com.aibuilt.ghostbank.data.db.HostLogEntity>,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            GhostTopBar(title = "Trace audit (${log.size})", onBack = onBack, actions = {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear log")
                }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("What is kept")
                    Text(
                        "Every exchange: original status, hop count, final URL and the full response body. A failed debit keeps its HTML so the next diagnosis is here, not in a screenshot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (log.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "Log is empty",
                        subtitle = "Run a harvest or a leak check to fill it"
                    )
                }
            } else {
                items(log, key = { it.id }) { entry ->
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(
                                "${entry.method} ${entry.status}",
                                when {
                                    entry.status in 200..399 -> MaterialTheme.colorScheme.primary
                                    entry.status == -1 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(entry.stage, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "hops ${entry.hops} in ${entry.elapsedMs}ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(entry.url, style = MaterialTheme.typography.bodySmall)
                        if (entry.finalUrl != entry.url) {
                            Text(
                                "final: ${entry.finalUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Text(
                            entry.requestHeaders,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MonoBlock(text = entry.responseBody, maxHeight = 180)
                    }
                }
            }
        }
    }
}
