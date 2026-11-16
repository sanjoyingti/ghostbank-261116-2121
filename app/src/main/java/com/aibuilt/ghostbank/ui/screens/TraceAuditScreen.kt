package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.HostLogEntity
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

@Composable
fun TraceAuditRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val log by vm.hostLog.collectAsStateWithLifecycle()
    TraceAuditScreen(entries = log, onBack = onBack, onClear = vm::clearLogs)
}

@Composable
fun TraceAuditScreen(
    entries: List<HostLogEntity>,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    Scaffold(
        topBar = {
            GhostTopBar(title = "Trace audit", onBack = onBack, actions = {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Text(
                    "Every exchange is kept with its original URL, hop count, final URL and the full body, so a failed run needs no screenshots. 3xx is alive; only 4xx/5xx is dead.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (entries.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Science,
                        title = "No traffic recorded",
                        subtitle = "Run a check, a save or a run and the requests appear here."
                    )
                }
            }

            items(entries, key = { it.id }) { entry ->
                var open by remember { mutableStateOf(false) }
                GhostCard(modifier = Modifier.clickable { open = !open }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${entry.method} ${entry.stage}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                entry.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            entry.status.toString(),
                            when {
                                entry.status in 200..399 -> MaterialTheme.colorScheme.primary
                                entry.status < 0 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    Text(
                        "hops ${entry.hops} - final ${entry.finalUrl} - ${entry.elapsedMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (open) {
                        Text("sent headers", style = MaterialTheme.typography.labelSmall)
                        MonoBlock(text = entry.requestHeaders, maxHeight = 120)
                        Text("response body", style = MaterialTheme.typography.labelSmall)
                        MonoBlock(
                            text = entry.responseBody.ifBlank { "empty body" },
                            maxHeight = 320
                        )
                    } else {
                        Text(
                            "tap to see the full body",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
