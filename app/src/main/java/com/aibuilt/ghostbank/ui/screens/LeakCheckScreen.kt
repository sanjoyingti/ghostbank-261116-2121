package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.aibuilt.ghostbank.ui.components.KeyValue
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
        onRun = vm::runLeakCheck,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeakCheckScreen(
    running: Boolean,
    findings: List<LeakFinding>,
    score: Int?,
    onRun: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { GhostTopBar(title = "Leak check", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("What the far end can see")
                    Text(
                        "Runs a real IP echo and a TLS fingerprint probe through the configured exit. Green = hidden, amber = physically visible or unavailable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onRun, enabled = !running, modifier = Modifier.fillMaxWidth()) {
                        Text(if (running) "Probing..." else "Run leak check")
                    }
                    if (running) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(6.dp))
                            Text("  Working through the probes", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            score?.let {
                item {
                    GhostCard {
                        KeyValue("Stealth score", "$it / 100")
                        Text(
                            "Score reasons are listed on the Stealth screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(findings, key = { it.label }) { finding ->
                GhostCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(finding.label, style = MaterialTheme.typography.titleMedium)
                            Text(finding.value, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                finding.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            if (finding.hidden) "hidden" else "visible",
                            if (finding.hidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            item {
                Text(
                    "If every finding is amber and no IP came back, no exit answered: the request never left the phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
