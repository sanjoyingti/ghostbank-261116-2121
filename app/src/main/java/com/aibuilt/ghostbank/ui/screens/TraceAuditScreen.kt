package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.HostLogEntity
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

    TraceAuditScreen(
        logs = logs,
        onClear = vm::clearLogs,
        onBack = onBack
    )
}

private fun statusColor(status: Int): androidx.compose.ui.graphics.Color = when {
    status in 200..399 -> androidx.compose.ui.graphics.Color(0xFF38F0C0)
    status in 400..499 -> androidx.compose.ui.graphics.Color(0xFFFFC857)
    status >= 500 -> androidx.compose.ui.graphics.Color(0xFFFF6B6B)
    else -> androidx.compose.ui.graphics.Color(0xFF9BB0C4)
}

@Composable
fun TraceAuditScreen(
    logs: List<HostLogEntity>,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            GhostTopBar(title = "Trace audit", onBack = onBack, actions = {
                IconButton(onClick = onClear, enabled = logs.isNotEmpty()) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear")
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
                    SectionTitle("What is kept")
                    Text(
                        "Every exchange with its original status, hop count, final URL and the full body - including an HTML error page from a failed debit leg. A blocked run stays visible so the next diagnosis needs no screenshot. Homepages and the canary treat 2xx and 3xx as alive; only 4xx and 5xx count as dead.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (logs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Shield,
                        title = "No traffic logged",
                        subtitle = "Run a harvest, a canary or the Leak Lab and every request lands here."
                    )
                }
            }

            items(logs, key = { it.id }) { log ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(
                            text = if (log.status < 0) "transport dead" else "HTTP ${log.status}",
                            color = statusColor(log.status)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            log.stage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    KeyValue("Method", log.method)
                    KeyValue("URL", log.url, MaterialTheme.colorScheme.onSurfaceVariant)
                    KeyValue("Final URL", log.finalUrl, MaterialTheme.colorScheme.onSurfaceVariant)
                    KeyValue("Hops", log.hops.toString())
                    KeyValue("Elapsed", "${log.elapsedMs} ms")
                    MonoBlock(log.requestHeaders, maxHeight = 90)
                    MonoBlock(log.responseBody.ifBlank { "(empty body)" }, maxHeight = 200)
                }
            }
        }
    }
}
