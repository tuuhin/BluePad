package com.sam.bluepad.database.data

import androidx.room.RoomDatabase

expect class AppDBBuilder {


    fun getDbBuilder(): RoomDatabase.Builder<BluePadDB>

    fun getMemoryDbBuilder(): RoomDatabase.Builder<BluePadDB>
}
