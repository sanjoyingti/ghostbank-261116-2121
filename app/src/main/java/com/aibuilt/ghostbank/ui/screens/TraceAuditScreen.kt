package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.MonoBlock
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

/** 2xx and 3xx count as alive here; only 4xx and 5xx are dead. */
private fun statusColor(status: Int): androidx.compose.ui.graphics.Color =
    when {
        status in 200..399 -> androidx.compose.ui.graphics.Color(0xFF38F0C0)
        status == -1 -> androidx.compose.ui.graphics.Color(0xFFFFC857)
        else -> androidx.compose.ui.graphics.Color(0xFFFF6B6B)
    }

@Composable
fun TraceAuditRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val logs by vm.hostLog.collectAsStateWithLifecycle()

    Scaffold(topBar = { GhostTopBar(title = "Trace audit", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("What actually left the device")
                    KeyValue("Exchanges logged", logs.size.toString())
                    KeyValue("Hops allowed", "5 per call, method and headers preserved")
                    Text(
                        "Original status, hop count and the final URL are kept together with the response body, " +
                            "so a failed leg is diagnosable without a screenshot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = vm::clearLogs, enabled = logs.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text("Clear log")
                    }
                }
            }

            if (logs.isEmpty()) {
                item {
                    GhostCard {
                        Text(
                            "Nothing logged yet. Save an account or run a harvest and every call shows up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(logs) { entry ->
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle(entry.stage)
                            StatusPill(if (entry.status == -1) "transport dead" else entry.status.toString(), statusColor(entry.status))
                            StatusPill("${entry.hops} hops")
                        }
                        KeyValue("Method", entry.method)
                        KeyValue("Requested", entry.url)
                        KeyValue("Final URL", entry.finalUrl)
                        KeyValue("Elapsed", "${entry.elapsedMs}ms")
                        MonoBlock(entry.responseBody.ifBlank { "(empty body)" })
                    }
                }
            }
        }
    }
}
