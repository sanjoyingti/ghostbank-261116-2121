package com.aibuilt.ghostbank.data.db

import androidx.room.TypeConverter
import com.aibuilt.ghostbank.model.HarvestState
import com.aibuilt.ghostbank.model.LedgerDirection

/**
 * Room cannot store Kotlin enums on its own, so both status enums are kept as their own name
 * text in the database. The DAO queries that compare against 'SETTLED' / 'CREDIT' literals
 * therefore line up exactly with what is written here.
 */
class Converters {

    @TypeConverter
    fun toHarvestState(value: String): HarvestState = HarvestState.valueOf(value)

    @TypeConverter
    fun fromHarvestState(value: HarvestState): String = value.name

    @TypeConverter
    fun toDirection(value: String): LedgerDirection = LedgerDirection.valueOf(value)

    @TypeConverter
    fun fromDirection(value: LedgerDirection): String = value.name
}
