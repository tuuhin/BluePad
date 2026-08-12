package com.sam.bluepad.data.database

import androidx.room3.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object AppDBConstructor : RoomDatabaseConstructor<BluePadDB> {
	override fun initialize(): BluePadDB
}
