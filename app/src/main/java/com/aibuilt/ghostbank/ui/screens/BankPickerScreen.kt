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
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
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
    BankPickerScreen(
        query = query,
        banks = vm.filtered(query),
        onQuery = vm::setQuery,
        onBack = onBack,
        onPick = onPick
    )
}

@Composable
fun BankPickerScreen(
    query: String,
    banks: List<com.aibuilt.ghostbank.model.Bank>,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onPick: (String) -> Unit
) {
    Scaffold(topBar = { GhostTopBar(title = "27 banks", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    label = { Text("Search name or code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(banks, key = { it.code }) { bank ->
                GhostCard {
                    Column {
                        Text(bank.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "IFSC prefix ${bank.code}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        KeyValue("Rail", bank.rail.label)
                        KeyValue("Per transfer cap", paiseLabel(bank.perTxCapPaise))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusPill(
                                "${bank.verifiedEndpoints.size} verified",
                                MaterialTheme.colorScheme.primary
                            )
                            StatusPill(
                                "${bank.probeOnlyEndpoints.size} probe only",
                                MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Button(
                            onClick = { onPick(bank.code) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Use this prefix") }
                    }
                }
            }
        }
    }
}
