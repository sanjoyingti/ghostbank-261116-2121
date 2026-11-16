package com.aibuilt.ghostbank.data.db

import androidx.room.TypeConverter
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection

/** Turns the two enum columns into plain text so Room can store them. */
class Converters {

    @TypeConverter
    fun fromHarvestState(state: HarvestState): String = state.name

    @TypeConverter
    fun toHarvestState(raw: String): HarvestState =
        runCatching { HarvestState.valueOf(raw) }.getOrDefault(HarvestState.PENDING)

    @TypeConverter
    fun fromLedgerDirection(direction: LedgerDirection): String = direction.name

    @TypeConverter
    fun toLedgerDirection(raw: String): LedgerDirection =
        runCatching { LedgerDirection.valueOf(raw) }.getOrDefault(LedgerDirection.DEBIT)
}
