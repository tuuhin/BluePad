package com.sam.bluepad.database.data.convertors

import androidx.room.TypeConverter
import kotlin.uuid.Uuid

internal class UUIDTypeConvertors {


    @TypeConverter
    fun fromUUIDToText(uuid: Uuid): String = uuid.toHexString()

    @TypeConverter
    fun fromTextToUUID(text: String): Uuid = Uuid.parseHex(text)
}
