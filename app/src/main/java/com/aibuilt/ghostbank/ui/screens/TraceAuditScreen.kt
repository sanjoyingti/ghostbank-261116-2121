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

    TraceAuditScreen(logs = logs, onClear = vm::clearLogs, onBack = onBack)
}

@Composable
fun TraceAuditScreen(
    logs: List<HostLogEntity>,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("What this is")
                    Text(
                        "Every exchange, with the original status, the hop count and the final address after redirects. A failed debit keeps its whole HTML body here, so the next diagnosis needs no screenshots.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (logs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.DeleteSweep,
                        title = "No traffic logged",
                        subtitle = "Run a harvest or a leak check and the exchanges show up here."
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    val alive = log.status in 200..399
                    GhostCard {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${log.stage} · ${log.method} · ${if (log.status < 0) "transport dead" else log.status.toString()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (alive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Text(
                                    log.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "hops ${log.hops} · ${log.elapsedMs}ms · ${fmt.format(Date(log.at))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (log.finalUrl.isNotBlank() && log.finalUrl != log.url) {
                                    Text(
                                        "final ${log.finalUrl}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                            StatusPill(
                                text = if (alive) "ALIVE" else "DEAD",
                                color = if (alive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        MonoBlock(log.responseBody.ifBlank { "(empty body)" }, maxHeight = 180)
                    }
                }
            }
        }
    }
}
