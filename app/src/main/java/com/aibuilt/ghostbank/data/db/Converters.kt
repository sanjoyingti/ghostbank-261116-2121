package com.aibuilt.ghostbank.data.db

import androidx.room.TypeConverter
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection

/**
 * The database cannot store Kotlin enums directly, but GhostDatabase was already built with
 * @@TypeConverters(Converters::class). These two converters turn the harvest state and the ledger
 * direction into their names on disk and back again.
 */
class Converters {

    @TypeConverter
    fun toHarvestState(value: String?): HarvestState? =
        value?.let { stored -> runCatching { HarvestState.valueOf(stored) }.getOrNull() }

    @TypeConverter
    fun fromHarvestState(state: HarvestState?): String? = state?.name

    @TypeConverter
    fun toDirection(value: String?): LedgerDirection? =
        value?.let { stored -> runCatching { LedgerDirection.valueOf(stored) }.getOrNull() }

    @TypeConverter
    fun fromDirection(direction: LedgerDirection?): String? = direction?.name
}
