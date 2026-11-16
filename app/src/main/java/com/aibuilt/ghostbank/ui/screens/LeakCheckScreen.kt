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
import androidx.compose.material3.LinearProgressIndicator
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
import com.aibuilt.ghostbank.model.LeakFinding
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
    LeakCheckScreen(
        running = leak.running,
        findings = leak.findings,
        score = leak.report?.score,
        onBack = onBack,
        onRun = vm::runLeakCheck
    )
}

@Composable
fun LeakCheckScreen(
    running: Boolean,
    findings: List<LeakFinding>,
    score: Int?,
    onBack: () -> Unit,
    onRun: () -> Unit
) {
    Scaffold(topBar = { GhostTopBar(title = "Leak check", onBack = onBack) }) { padding ->
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
                    SectionTitle("What the far end can see")
                    Text(
                        "Real IP echo and TLS fingerprint endpoints are hit for real - nothing is simulated. Run it before a harvest.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onRun, enabled = !running) {
                            Text(if (running) "Checking..." else "Run leak check")
                        }
                        if (running) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    }
                    if (score != null) {
                        Text("Stealth score $score / 100", style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { score / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (findings.isEmpty() && !running) {
                item {
                    Text(
                        "No results yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(findings.size) { index ->
                val finding = findings[index]
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(finding.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                finding.value,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (finding.hidden) {
                            StatusPill("hidden", MaterialTheme.colorScheme.primary)
                        } else {
                            StatusPill("exposed", MaterialTheme.colorScheme.tertiary)
                        }
                    }
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
