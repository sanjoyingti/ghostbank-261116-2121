package com.aibuilt.ghostbank.data.db

import androidx.room.TypeConverter
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection

/** Stores the two enums as their names so the tables stay readable in the raw log. */
class Converters {

    @TypeConverter
    fun fromHarvestState(state: HarvestState?): String? = state?.name

    @TypeConverter
    fun toHarvestState(value: String?): HarvestState? =
        value?.let { runCatching { HarvestState.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromLedgerDirection(direction: LedgerDirection?): String? = direction?.name

    @TypeConverter
    fun toLedgerDirection(value: String?): LedgerDirection? =
        value?.let { runCatching { LedgerDirection.valueOf(it) }.getOrNull() }
}
