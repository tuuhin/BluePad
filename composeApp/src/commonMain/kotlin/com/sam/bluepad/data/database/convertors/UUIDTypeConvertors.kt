package com.sam.bluepad.data.database.convertors

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import kotlin.uuid.Uuid

@ProvidedColumnTypeConverter
class UUIDTypeConvertors {

    @ColumnTypeConverter
	fun fromUUIDToText(uuid: Uuid): String = uuid.toHexString()

    @ColumnTypeConverter
	fun fromTextToUUID(text: String): Uuid = Uuid.parseHex(text)

}
