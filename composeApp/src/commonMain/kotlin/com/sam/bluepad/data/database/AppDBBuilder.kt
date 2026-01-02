package com.sam.bluepad.data.database

import androidx.room.RoomDatabase

expect class AppDBBuilder {

	fun getDbBuilder(): RoomDatabase.Builder<BluePadDB>

}