package com.aibuilt.ghostbank.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.LedgerEntity
import com.aibuilt.ghostbank.model.LedgerDirection
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.components.paiseLabel
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerRoute(container: AppContainer) {
    val vm: LedgerViewModel = viewModel(factory = ghostFactory { LedgerViewModel(container) })
    val rows by vm.rows.collectAsStateWithLifecycle()
    val credited by vm.creditedPaise.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LedgerScreen(
        rows = rows,
        creditedPaise = credited,
        onExport = {
            scope.launch {
                val intent = vm.export(context)
                if (intent != null) {
                    context.startActivity(Intent.createChooser(intent, "Export ledger"))
                }
            }
        },
        onClear = vm::clear
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    rows: List<LedgerEntity>,
    creditedPaise: Long,
    onExport: () -> Unit,
    onClear: () -> Unit
) {
    val fmt = SimpleDateFormat("dd MMM HH:mm:ss", Locale.US)

    Scaffold(
        topBar = {
            GhostTopBar(title = "Ledger", actions = {
                IconButton(onClick = onExport, enabled = rows.isNotEmpty()) {
                    Icon(Icons.Default.Share, contentDescription = "Export CSV")
                }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GhostCard {
                    KeyValue("Credited to destination", "$creditedPaise paise (${paiseLabel(creditedPaise)})")
                    KeyValue("Rows", rows.size.toString())
                    Text(
                        "The balance is held in whole paise so 25 harvests read as 25 paise and nothing is rounded away.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExport, enabled = rows.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Text("Export CSV")
                    }
                    OutlinedButton(onClick = onClear, enabled = rows.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Text("Clear")
                    }
                }
            }

            if (rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "Ledger is empty",
                        subtitle = "Debit and credit rows appear here only after a run books a UTR."
                    )
                }
            }

            items(rows, key = { it.id }) { row ->
                GhostCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.bankName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                fmt.format(Date(row.bookedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(row.note, style = MaterialTheme.typography.bodySmall)
                            row.utr?.let { Text("UTR $it", style = MaterialTheme.typography.bodySmall) }
                        }
                        Column {
                            StatusPill(
                                row.direction.name,
                                if (row.direction == LedgerDirection.CREDIT) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )
                            Text(
                                "${row.amountPaise} p",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
