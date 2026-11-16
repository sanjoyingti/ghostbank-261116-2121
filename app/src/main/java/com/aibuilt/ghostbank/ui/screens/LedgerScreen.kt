package com.aibuilt.ghostbank.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
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
    val fmt = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            GhostTopBar(title = "Ledger", actions = {
                IconButton(onClick = {
                    scope.launch {
                        val intent = vm.export(context)
                        if (intent != null) {
                            context.startActivity(
                                Intent.createChooser(intent, "Export ledger").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Credited to destination")
                    Text(
                        "$credited paise (${paiseLabel(credited)}) across ${rows.count { it.direction == LedgerDirection.CREDIT }} credit rows",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    TextButton(onClick = vm::clear) { Text("Clear ledger") }
                }
            }

            if (rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "Nothing booked yet",
                        subtitle = "Rows appear only after a debit confirms and a UTR is captured."
                    )
                }
            } else {
                items(rows, key = { it.id }) { row ->
                    GhostCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                fmt.format(Date(row.bookedAt)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(
                                text = row.direction.name,
                                color = if (row.direction == LedgerDirection.CREDIT) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                        KeyValue("Bank", row.bankName)
                        KeyValue("Amount", "${row.amountPaise} paise")
                        row.utr?.let { KeyValue("UTR", it) }
                        KeyValue("Note", row.note)
                    }
                }
            }
        }
    }
}
