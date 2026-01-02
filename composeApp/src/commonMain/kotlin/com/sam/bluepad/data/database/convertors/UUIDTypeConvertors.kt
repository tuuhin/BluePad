package com.sam.bluepad.data.database.convertors

import androidx.room.TypeConverter
import kotlin.uuid.Uuid

class UUIDTypeConvertors {

	@TypeConverter
	fun fromUUIDToText(uuid: Uuid): String = uuid.toHexString()

	@TypeConverter
	fun fromTextToUUID(text: String): Uuid = Uuid.parseHex(text)

}