package com.aibuilt.ghostbank.data.db

import androidx.room.TypeConverter
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection
import com.aibuilt.ghostbank.model.StealthLevel

/** Room cannot store enums by itself; these keep the exact status names in the tables. */
class Converters {

    @TypeConverter
    fun toHarvestState(value: String?): HarvestState? {
        if (value == null) return null
        return runCatching { HarvestState.valueOf(value) }.getOrNull()
    }

    @TypeConverter
    fun fromHarvestState(value: HarvestState?): String? = value?.name

    @TypeConverter
    fun toDirection(value: String?): LedgerDirection? {
        if (value == null) return null
        return runCatching { LedgerDirection.valueOf(value) }.getOrNull()
    }

    @TypeConverter
    fun fromDirection(value: LedgerDirection?): String? = value?.name

    @TypeConverter
    fun toLevel(value: String?): StealthLevel? {
        if (value == null) return null
        return runCatching { StealthLevel.valueOf(value) }.getOrNull()
    }

    @TypeConverter
    fun fromLevel(value: StealthLevel?): String? = value?.name
}
