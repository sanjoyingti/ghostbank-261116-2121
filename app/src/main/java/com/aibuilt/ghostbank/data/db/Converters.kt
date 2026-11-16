package com.aibuilt.ghostbank.data.db

import androidx.room.TypeConverter
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection

/** Stores the two enums as their names so SQL can compare them as plain text. */
class Converters {

    @TypeConverter
    fun fromHarvestState(value: HarvestState?): String? = value?.name

    @TypeConverter
    fun toHarvestState(value: String?): HarvestState? =
        value?.let { runCatching { HarvestState.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromDirection(value: LedgerDirection?): String? = value?.name

    @TypeConverter
    fun toDirection(value: String?): LedgerDirection? =
        value?.let { runCatching { LedgerDirection.valueOf(it) }.getOrNull() }
}
