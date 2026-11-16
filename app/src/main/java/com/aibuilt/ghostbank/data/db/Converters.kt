package com.aibuilt.ghostbank.data.db

import androidx.room.TypeConverter
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection

/** Room cannot store enums directly, so they are written as their stable names. */
class Converters {

    @TypeConverter
    fun harvestStateToString(value: HarvestState): String = value.name

    @TypeConverter
    fun stringToHarvestState(value: String): HarvestState =
        runCatching { HarvestState.valueOf(value) }.getOrDefault(HarvestState.PENDING)

    @TypeConverter
    fun directionToString(value: LedgerDirection): String = value.name

    @TypeConverter
    fun stringToDirection(value: String): LedgerDirection =
        runCatching { LedgerDirection.valueOf(value) }.getOrDefault(LedgerDirection.DEBIT)
}
