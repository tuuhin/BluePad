package com.sam.bluepad.data.database.convertors

import androidx.room.TypeConverter
import kotlin.time.Instant

class InstantTypeConvertor {

	@TypeConverter
	fun fromInstantToMillis(from: Instant): Long = from.toEpochMilliseconds()

	@TypeConverter
	fun toMillisFromInstant(from: Long): Instant = Instant.fromEpochMilliseconds(from)
}