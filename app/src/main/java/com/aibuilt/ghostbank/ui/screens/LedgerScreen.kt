package com.aibuilt.ghostbank.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
import com.aibuilt.ghostbank.ui.components.SectionTitle
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
                    val chooser = Intent.createChooser(intent, "Export ledger")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            }
        },
        onClear = vm::clear
    )
}

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
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.Share, contentDescription = "Export CSV")
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear ledger")
                }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Booked credits")
                    KeyValue("Total credited", paiseLabel(creditedPaise))
                    KeyValue("Rows", rows.size.toString())
                    Text(
                        "Debit and credit rows are written side by side only after a settlement reference exists.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "Nothing booked yet",
                        subtitle = "A run books here only once the rail returns a final status."
                    )
                }
            }

            items(rows, key = { it.id }) { row ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${row.direction.name} ${paiseLabel(row.amountPaise)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${row.bankName} - ${fmt.format(Date(row.bookedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            row.direction.name,
                            if (row.direction == LedgerDirection.CREDIT) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                    Text(
                        "utr ${row.utr ?: "none"} - ${row.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
