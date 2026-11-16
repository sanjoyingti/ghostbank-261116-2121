package com.aibuilt.ghostbank.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.LedgerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LedgerViewModel(private val container: AppContainer) : ViewModel() {

    val rows: StateFlow<List<LedgerEntity>> = container.ledgerDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditedPaise: StateFlow<Long> = container.ledgerDao.observeCreditedPaise()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun clear() {
        viewModelScope.launch { container.ledgerDao.clearAll() }
    }

    /** Export CSV to cache and hand back a share intent, or null when there is nothing to send. */
    suspend fun export(context: Context): Intent? = withContext(Dispatchers.IO) {
        val data = rows.value
        if (data.isEmpty()) return@withContext null
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder("date,direction,bank,amount_paise,utr,note\n")
        data.forEach { row ->
            sb.append(fmt.format(Date(row.bookedAt))).append(',')
                .append(row.direction.name).append(',')
                .append(row.bankName.replace(",", " ")).append(',')
                .append(row.amountPaise).append(',')
                .append(row.utr ?: "").append(',')
                .append(row.note.replace(",", " ")).append('\n')
        }
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "ghostbank_ledger.csv")
        file.writeText(sb.toString())
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
