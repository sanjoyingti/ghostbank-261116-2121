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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.aibuilt.ghostbank.model.Bank
import com.aibuilt.ghostbank.model.EndpointState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.paiseLabel
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.BankViewModel

@Composable
fun BankPickerRoute(
    container: AppContainer,
    onBack: () -> Unit,
    onPick: (String) -> Unit
) {
    val vm: BankViewModel = viewModel(factory = ghostFactory { BankViewModel() })
    val query by vm.query.collectAsStateWithLifecycle()
    val banks = vm.filtered(query)
    Scaffold(topBar = { GhostTopBar(title = "Bank picker", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = { Text("Search bank or IFSC prefix") },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
            if (banks.isEmpty()) {
                GhostCard {
                    SectionTitle("No match")
                    Text(
                        "No registered bank carries that name or prefix.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(banks) { bank -> BankRow(bank = bank, onPick = onPick) }
                }
            }
        }
    }
}

@Composable
private fun BankRow(bank: Bank, onPick: (String) -> Unit) {
    GhostCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(bank.name)
            StatusPill(bank.code)
        }
        KeyValue("Per-transaction cap", paiseLabel(bank.perTxCapPaise))
        KeyValue("Rail", bank.rail.label)
        KeyValue("Site", bank.homepage)
        val verified = bank.endpoints.count { it.state == EndpointState.VERIFIED }
        val probeOnly = bank.endpoints.size - verified
        KeyValue("Verified endpoints", verified.toString(), MaterialTheme.colorScheme.primary)
        KeyValue(
            "Probe-only endpoints",
            probeOnly.toString(),
            if (probeOnly > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
        )
        androidx.compose.material3.OutlinedButton(
            onClick = { onPick(bank.code) },
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Icon(Icons.Default.AccountBalance, contentDescription = null)
            Text(" Use this bank")
        }
    }
}
