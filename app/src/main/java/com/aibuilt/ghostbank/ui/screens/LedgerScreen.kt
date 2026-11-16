package com.aibuilt.ghostbank.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.font.FontFamily
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

    Scaffold(
        topBar = {
            GhostTopBar(title = "Ledger", actions = {
                IconButton(onClick = {
                    scope.launch {
                        vm.export(context)?.let { intent ->
                            context.startActivity(Intent.createChooser(intent, "Export ledger"))
                        }
                    }
                }) { Icon(Icons.Default.Share, contentDescription = "Export") }
                IconButton(onClick = { vm.clear() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Credited total")
                    KeyValue("Paise summed", "${credited} paise")
                    KeyValue("As money", paiseLabel(credited))
                    Text(
                        "Every row is written only after the bank confirmed; nothing is rounded away.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Share,
                        title = "Ledger is empty",
                        subtitle = "Settled legs appear here with their UTR."
                    )
                }
            }

            items(rows, key = { it.id }) { row ->
                val isCredit = row.direction == LedgerDirection.CREDIT
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            StatusPill(
                                if (isCredit) "CREDIT" else "DEBIT",
                                if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                            Text(row.bankName, style = MaterialTheme.typography.titleMedium)
                            Text(row.note, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "UTR ${row.utr ?: "none"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US).format(Date(row.bookedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(paiseLabel(row.amountPaise), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
