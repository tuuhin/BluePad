package com.sam.bluepad.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class AppDBBuilder(private val context: Context) {

    actual fun getDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(BluePadDB.APP_DB_NAME)
        return Room.databaseBuilder<BluePadDB>(context = appContext, name = dbFile.absolutePath)
    }

    actual fun getMemoryDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        return Room.inMemoryDatabaseBuilder<BluePadDB>(context)
    }
}
