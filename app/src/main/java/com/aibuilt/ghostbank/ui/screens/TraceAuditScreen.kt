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
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TraceAuditRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val logs by vm.hostLog.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GhostTopBar(title = "Trace audit", onBack = onBack, actions = {
                IconButton(onClick = vm::clearLogs) {
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
                GhostCard {
                    SectionTitle("What the far end saw")
                    Text(
                        "2xx and 3xx count as alive. The original status, the hop count and the final URL are kept so the next diagnosis needs no screenshot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (logs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.TravelExplore,
                        title = "No traffic recorded",
                        subtitle = "Every request lands here with its raw body."
                    )
                }
            }

            items(logs, key = { it.id }) { log -> LogRow(log) }
        }
    }
}

@Composable
private fun LogRow(log: HostLogEntity) {
    val color = when {
        log.status in 200..399 -> MaterialTheme.colorScheme.primary
        log.status < 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    GhostCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatusPill(text = "status ${log.status}", color = color)
            StatusPill(text = "${log.hops} hop(s)", color = MaterialTheme.colorScheme.secondary)
            StatusPill(text = log.stage, color = MaterialTheme.colorScheme.outline)
        }
        KeyValue("Method", log.method)
        Text(
            log.url,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (log.finalUrl != log.url) {
            Text(
                "final: ${log.finalUrl}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()).format(Date(log.at)) +
                " - ${log.elapsedMs}ms",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MonoBlock(text = log.responseBody.ifBlank { "(empty body)" }, maxHeight = 200)
    }
}
