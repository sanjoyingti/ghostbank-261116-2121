package com.aibuilt.ghostbank.data.db

import androidx.room.TypeConverter
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection

/** Stores the two enums as plain text so the columns stay readable in the raw log. */
class Converters {

    @TypeConverter
    fun toHarvestState(value: String): HarvestState =
        runCatching { HarvestState.valueOf(value) }.getOrDefault(HarvestState.PENDING)

    @TypeConverter
    fun fromHarvestState(value: HarvestState): String = value.name

    @TypeConverter
    fun toDirection(value: String): LedgerDirection =
        runCatching { LedgerDirection.valueOf(value) }.getOrDefault(LedgerDirection.DEBIT)

    @TypeConverter
    fun fromDirection(value: LedgerDirection): String = value.name
}
