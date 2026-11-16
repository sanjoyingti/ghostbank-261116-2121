package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
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
import com.aibuilt.ghostbank.data.BankRegistry
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
                    label = { Text("Search bank or IFSC code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    "Tapping a bank fills its IFSC prefix into the account form. Verified routes only: the resolver is the single live-proven endpoint.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(vm.filtered(query), key = { it.code }) { bank ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "  ${bank.name}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(bank.code)
                    }
                    KeyValue("Rail", bank.rail.label)
                    KeyValue("Per-transaction cap", paiseLabel(bank.perTxCapPaise))
                    KeyValue("Verified routes", bank.verifiedEndpoints.size.toString())
                    KeyValue(
                        "Probe-only routes",
                        bank.probeOnlyEndpoints.size.toString(),
                        MaterialTheme.colorScheme.tertiary
                    )
                    SectionTitle("Use this bank")
                    bank.verifiedEndpoints.firstOrNull { it.state == EndpointState.VERIFIED }?.let {
                        Text(
                            "Verified: ${it.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = { onPick(bank.code) }) {
                        Text("Prefill ${bank.code}0...")
                    }
                }
            }
            item {
                Text(
                    "Registry size: ${BankRegistry.banks.size} banks, ${BankRegistry.verifiedDebitEndpoints.size} verified routes, ${BankRegistry.probeOnlyEndpoints.size} probe-only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
