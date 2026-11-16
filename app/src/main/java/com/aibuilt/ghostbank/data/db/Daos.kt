package com.aibuilt.ghostbank.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aibuilt.ghostbank.model.HarvestState
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(account: AccountEntity): Long

    @Query("SELECT * FROM accounts ORDER BY isDestination DESC, createdAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isDestination DESC, createdAt DESC")
    suspend fun all(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE fingerprint = :fp LIMIT 1")
    suspend fun byFingerprint(fp: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isDestination = 1 LIMIT 1")
    fun observeDestination(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE isDestination = 1 LIMIT 1")
    suspend fun destination(): AccountEntity?

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE accounts SET isDestination = 0")
    suspend fun clearDestination()

    @Query("UPDATE accounts SET isDestination = 1 WHERE id = :id")
    suspend fun setDestination(id: Long)
}

@Dao
interface HarvestDao {
    @Insert
    suspend fun insert(harvest: HarvestEntity): Long

    @Update
    suspend fun update(harvest: HarvestEntity)

    @Query("SELECT * FROM harvests WHERE id = :id")
    suspend fun byId(id: Long): HarvestEntity?

    @Query("SELECT * FROM harvests WHERE runId = :runId ORDER BY id ASC")
    fun observeRun(runId: String): Flow<List<HarvestEntity>>

    @Query("SELECT * FROM harvests ORDER BY createdAt DESC LIMIT 300")
    fun observeRecent(): Flow<List<HarvestEntity>>

    @Query("SELECT * FROM harvests WHERE state IN ('SETTLED','FLOOR','HARVESTED') ORDER BY createdAt DESC LIMIT 500")
    fun observeSettled(): Flow<List<HarvestEntity>>

    @Query("SELECT COALESCE(SUM(amountPaise),0) FROM harvests WHERE state IN ('SETTLED','FLOOR','HARVESTED')")
    fun observeTotalPaise(): Flow<Long>

    @Query("SELECT * FROM harvests WHERE accountId = :accountId AND state IN ('SETTLED','FLOOR','HARVESTED') LIMIT 1")
    suspend fun harvestedFor(accountId: Long): HarvestEntity?

    @Query("SELECT * FROM harvests WHERE accountId = :accountId AND state = 'FLOOR' LIMIT 1")
    suspend fun floorFor(accountId: Long): HarvestEntity?

    @Query("UPDATE harvests SET state = :state, reason = :reason, updatedAt = :now WHERE id = :id")
    suspend fun setState(id: Long, state: HarvestState, reason: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE harvests SET utr = :utr, updatedAt = :now WHERE id = :id")
    suspend fun setUtr(id: Long, utr: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE harvests SET reason = :reason, updatedAt = :now WHERE id = :id")
    suspend fun setReason(id: Long, reason: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM harvests")
    suspend fun clearAll()
}

@Dao
interface LedgerDao {
    @Insert
    suspend fun insert(row: LedgerEntity): Long

    @Query("SELECT * FROM ledger ORDER BY bookedAt DESC LIMIT 500")
    fun observeAll(): Flow<List<LedgerEntity>>

    @Query("SELECT COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amountPaise ELSE 0 END),0) FROM ledger")
    fun observeCreditedPaise(): Flow<Long>

    @Query("DELETE FROM ledger")
    suspend fun clearAll()
}

@Dao
interface HostLogDao {
    @Insert
    suspend fun insert(log: HostLogEntity): Long

    @Query("SELECT * FROM host_log WHERE runId = :runId ORDER BY id ASC")
    fun observeRun(runId: String): Flow<List<HostLogEntity>>

    @Query("SELECT * FROM host_log ORDER BY id DESC LIMIT 400")
    fun observeRecent(): Flow<List<HostLogEntity>>

    @Query("DELETE FROM host_log")
    suspend fun clearAll()
}

@Dao
interface VirtualAccountDao {
    @Insert
    suspend fun insert(va: VirtualAccountEntity): Long

    @Query("SELECT * FROM virtual_accounts WHERE active = 1 ORDER BY uses ASC, id ASC")
    suspend fun active(): List<VirtualAccountEntity>

    @Query("SELECT * FROM virtual_accounts ORDER BY id ASC")
    fun observeAll(): Flow<List<VirtualAccountEntity>>

    @Query("UPDATE virtual_accounts SET uses = uses + 1 WHERE id = :id")
    suspend fun bumpUse(id: Long)

    @Query("UPDATE virtual_accounts SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM virtual_accounts WHERE id = :id")
    suspend fun delete(id: Long)
}
