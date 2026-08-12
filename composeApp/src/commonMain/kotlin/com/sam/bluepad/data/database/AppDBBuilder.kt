package com.sam.bluepad.data.database

import androidx.room3.RoomDatabase


expect class AppDBBuilder {

    fun getDbBuilder(): RoomDatabase.Builder<BluePadDB>

    fun getMemoryDbBuilder(): RoomDatabase.Builder<BluePadDB>
}
