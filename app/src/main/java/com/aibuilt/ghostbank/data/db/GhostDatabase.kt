package com.aibuilt.ghostbank.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        HarvestEntity::class,
        LedgerEntity::class,
        HostLogEntity::class,
        VirtualAccountEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GhostDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun harvestDao(): HarvestDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun hostLogDao(): HostLogDao
    abstract fun virtualAccountDao(): VirtualAccountDao
}
