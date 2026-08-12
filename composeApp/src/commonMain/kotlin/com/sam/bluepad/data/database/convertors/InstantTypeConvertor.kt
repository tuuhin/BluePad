package com.sam.bluepad.data.database.convertors

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import kotlin.time.Instant

@ProvidedColumnTypeConverter
class InstantTypeConvertor {

    @ColumnTypeConverter
	fun fromInstantToMillis(from: Instant): Long = from.toEpochMilliseconds()

    @ColumnTypeConverter
	fun toMillisFromInstant(from: Long): Instant = Instant.fromEpochMilliseconds(from)
}
