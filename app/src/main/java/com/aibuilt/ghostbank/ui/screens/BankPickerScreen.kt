package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
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
                    onValueChange = onQuery,
                    label = { Text("Search name or code") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    "${banks.size} banks. A tap fills the IFSC prefix so you only type the branch part. Per-bank caps are shown.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(banks, key = { it.code }) { bank ->
                GhostCard(modifier = Modifier.clickable { onPick(bank.code) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bank.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${bank.code}0 - ${bank.rail.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill("cap ${paiseLabel(bank.perTxCapPaise)}", MaterialTheme.colorScheme.secondary)
                    }
                    val verified = bank.verifiedEndpoints.size
                    val total = bank.endpoints.size
                    Text(
                        "$verified verified / $total registered endpoints",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (verified > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                    SectionTitle(bank.homepage)
                }
            }
        }
    }
}
