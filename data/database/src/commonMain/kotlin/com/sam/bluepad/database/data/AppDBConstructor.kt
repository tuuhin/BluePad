package com.sam.bluepad.database.data

import androidx.room.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object AppDBConstructor : RoomDatabaseConstructor<BluePadDB> {


    override fun initialize(): BluePadDB
}
