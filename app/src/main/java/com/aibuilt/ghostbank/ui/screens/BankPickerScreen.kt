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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.model.Bank
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.paiseLabel
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.BankViewModel

@Composable
fun BankPickerRoute(container: AppContainer, onBack: () -> Unit, onPick: (String) -> Unit) {
    val vm: BankViewModel = viewModel(factory = ghostFactory { BankViewModel() })
    val query by vm.query.collectAsStateWithLifecycle()

    BankPickerScreen(
        query = query,
        onQuery = vm::setQuery,
        banks = vm.filtered(query),
        totalBanks = vm.banks.size,
        onBack = onBack,
        onPick = onPick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankPickerScreen(
    query: String,
    onQuery: (String) -> Unit,
    banks: List<Bank>,
    totalBanks: Int,
    onBack: () -> Unit,
    onPick: (String) -> Unit
) {
    Scaffold(topBar = { GhostTopBar(title = "Banks ($totalBanks)", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    label = { Text("Search bank or IFSC prefix") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    "Picking a bank fills its IFSC prefix into the account form.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (banks.isEmpty()) {
                item {
                    GhostCard {
                        Text("No bank matches \"$query\".", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(banks, key = { it.code }) { bank ->
                GhostCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bank.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "IFSC prefix ${bank.code}0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(bank.rail.label, MaterialTheme.colorScheme.secondary)
                    }
                    KeyValue("Per-transfer cap", paiseLabel(bank.perTxCapPaise))
                    KeyValue("Site", bank.homepage)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill("${bank.verifiedEndpoints.size} verified", MaterialTheme.colorScheme.primary)
                        StatusPill("${bank.probeOnlyEndpoints.size} probe-only", MaterialTheme.colorScheme.tertiary)
                    }
                    bank.endpoints.take(3).forEach { endpoint ->
                        Text(
                            "${if (endpoint.state.name == "VERIFIED") "verified" else "probe ${endpoint.probeStatus}"} - ${endpoint.urlTemplate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onPick(bank.code) }) { Text("Use ${bank.code}0 prefix") }
                }
            }
            item { SectionTitle("Per-bank caps are enforced, never exceeded") }
        }
    }
}
