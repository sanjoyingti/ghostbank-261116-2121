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
import androidx.compose.ui.text.font.FontFamily
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
    banks: List<Bank>,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onPick: (String) -> Unit
) {
    Scaffold(topBar = { GhostTopBar(title = "Bank picker", onBack = onBack) }) { padding ->
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
                    onValueChange = onQuery,
                    label = { Text("Search name or IFSC prefix") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    "Tapping a bank fills its IFSC prefix (4 letters + 0). Only routes proven by a live probe are allowed to carry a debit leg; the rest stay probe-only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(banks, key = { it.code }) { bank ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bank.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${bank.code}0 - ${bank.rail.label}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(paiseLabel(bank.perTxCapPaise), MaterialTheme.colorScheme.secondary)
                    }
                    KeyValue("Homepage", bank.homepage)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            "${bank.verifiedEndpoints.size} verified",
                            MaterialTheme.colorScheme.primary
                        )
                        StatusPill(
                            "${bank.probeOnlyEndpoints.size} probe-only",
                            MaterialTheme.colorScheme.tertiary
                        )
                    }
                    bank.verifiedEndpoints.firstOrNull()?.let { ep ->
                        Text(
                            ep.urlTemplate,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SectionTitle("Use this bank")
                    androidx.compose.material3.TextButton(onClick = { onPick(bank.code + "0") }) {
                        Text("Fill IFSC prefix ${bank.code}0")
                    }
                }
            }
        }
    }
}
