package com.sam.bluepad.database.data.convertors

import androidx.room.TypeConverter
import kotlin.time.Instant

internal class InstantTypeConvertor {


    @TypeConverter
    fun fromInstantToMillis(from: Instant): Long = from.toEpochMilliseconds()

    @TypeConverter
    fun toMillisFromInstant(from: Long): Instant = Instant.fromEpochMilliseconds(from)
}
