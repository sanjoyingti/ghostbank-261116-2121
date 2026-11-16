package com.aibuilt.ghostbank.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection

@Entity(tableName = "accounts", indices = [Index(value = ["fingerprint"], unique = true)])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountNumber: String,
    val ifsc: String,
    val bankCode: String,
    val bankName: String,
    val holderLabel: String,
    val fingerprint: String,
    val isDestination: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "harvests", indices = [Index(value = ["accountId"]), Index(value = ["runId"])])
data class HarvestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val accountId: Long,
    val accountNumber: String,
    val ifsc: String,
    val bankCode: String,
    val bankName: String,
    val amountPaise: Long,
    val state: HarvestState,
    val reason: String = "",
    val utr: String? = null,
    val idempotencyKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ledger")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val direction: LedgerDirection,
    val amountPaise: Long,
    val bankCode: String,
    val bankName: String,
    val utr: String?,
    val note: String,
    val bookedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "host_log", indices = [Index(value = ["runId"])])
data class HostLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val stage: String,
    val method: String,
    val url: String,
    val finalUrl: String,
    val status: Int,
    val hops: Int,
    val elapsedMs: Long,
    val requestHeaders: String,
    val responseBody: String,
    val at: Long = System.currentTimeMillis()
)

@Entity(tableName = "virtual_accounts")
data class VirtualAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountNumber: String,
    val ifsc: String,
    val bankCode: String,
    val label: String,
    val uses: Int = 0,
    val active: Boolean = true
)
