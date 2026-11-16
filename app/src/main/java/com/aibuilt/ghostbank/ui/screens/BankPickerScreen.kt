package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
fun BankPickerRoute(container: AppContainer, onBack: () -> Unit, onPick: (String) -> Unit) {
    val vm: BankViewModel = viewModel(factory = ghostFactory { BankViewModel() })
    val query by vm.query.collectAsStateWithLifecycle()
    val banks = vm.filtered(query)

    Scaffold(topBar = { GhostTopBar(title = "Banks", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    label = { Text("Search name or IFSC prefix") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    "${vm.banks.size} banks registered. Per-bank cap shown; the verified resolver is shared by all of them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(banks, key = { it.code }) { bank ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(bank.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        StatusPill(bank.code, MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.weight(1f))
                        if (bank.verifiedEndpoints.isNotEmpty()) {
                            StatusPill("VERIFIED route", MaterialTheme.colorScheme.primary)
                        }
                        if (bank.probeOnlyEndpoints.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            StatusPill("${bank.probeOnlyEndpoints.size} probe-only", MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    KeyValue("Rail", bank.rail.label)
                    KeyValue("Per-txn cap", paiseLabel(bank.perTxCapPaise))
                    KeyValue("Homepage", bank.homepage)
                    bank.endpoints.forEach { ep ->
                        KeyValue(
                            ep.id,
                            "${ep.state.name} status=${ep.probeStatus ?: "-"}",
                            if (ep.state.name == "VERIFIED") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = { onPick(bank.code) }) {
                        Text("Use this bank")
                    }
                }
            }
        }
    }
}
