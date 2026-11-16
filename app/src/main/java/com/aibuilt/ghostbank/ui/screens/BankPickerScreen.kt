package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.aibuilt.ghostbank.ui.components.GhostTopBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankPickerScreen(
    query: String,
    banks: List<Bank>,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onPick: (String) -> Unit
) {
    Scaffold(topBar = { GhostTopBar(title = "Banks (${banks.size})", onBack = onBack) }) { padding ->
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
                    label = { Text("Search name or code") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(banks, key = { it.code }) { bank ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPick(bank.code) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(bank.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusPill(bank.code)
                        }
                        Text(
                            "IFSC prefix ${bank.code}0  -  cap ${paiseLabel(bank.perTxCapPaise)}  -  ${bank.rail.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${bank.verifiedEndpoints.size} verified endpoint(s), ${bank.probeOnlyEndpoints.size} probe-only, homepage ${bank.homepage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
