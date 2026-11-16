package com.aibuilt.ghostbank.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
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
    val fmt = SimpleDateFormat("dd MMM HH:mm:ss", Locale.US)

    Scaffold(
        topBar = {
            GhostTopBar(title = "Ledger", actions = {
                IconButton(onClick = {
                    scope.launch {
                        vm.export(context)?.let { intent ->
                            context.startActivity(Intent.createChooser(intent, "Export ledger"))
                        }
                    }
                }) { Icon(Icons.Default.Share, contentDescription = "Export CSV") }
                TextButton(onClick = vm::clear) { Text("Clear") }
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
                    SectionTitle("Exact paise ledger")
                    KeyValue("Total credited", "${paiseLabel(credited)} = $credited paise")
                    KeyValue("Rows", rows.size.toString())
                    Text(
                        "Amounts stay integers in paise, so 25 harvests read as 25 paise, never rounded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (rows.isEmpty()) {
                item { EmptyState(Icons.Default.ReceiptLong, "Empty ledger", "Settled harvests are booked here with their UTR.") }
            }

            items(rows, key = { it.id }) { row ->
                GhostCard {
                    Row {
                        StatusPill(
                            row.direction.name,
                            if (row.direction == LedgerDirection.CREDIT) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(paiseLabel(row.amountPaise), style = MaterialTheme.typography.titleMedium)
                    }
                    KeyValue("Bank", row.bankName)
                    KeyValue("UTR", row.utr ?: "not issued")
                    KeyValue("Note", row.note)
                    KeyValue("Time", fmt.format(Date(row.bookedAt)))
                }
            }
        }
    }
}
