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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel

@Composable
fun LeakCheckRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val leak by vm.leak.collectAsStateWithLifecycle()

    Scaffold(topBar = { GhostTopBar(title = "Leak check", onBack = onBack) }) { padding ->
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
                    SectionTitle("Before you run anything")
                    Text(
                        "This hits a real IP echo and a TLS fingerprint service so you can see exactly what the bank would see. Green is hidden, amber is a physical limit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = vm::runLeakCheck,
                        enabled = !leak.running,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Run leak check") }
                    if (leak.running) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                            Text("  probing real endpoints...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            items(leak.findings, key = { it.label }) { finding ->
                GhostCard {
                    Column {
                        StatusPill(
                            if (finding.hidden) "HIDDEN" else "VISIBLE",
                            if (finding.hidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                        Text(finding.label, style = MaterialTheme.typography.titleMedium)
                        Text(finding.value, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            finding.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
