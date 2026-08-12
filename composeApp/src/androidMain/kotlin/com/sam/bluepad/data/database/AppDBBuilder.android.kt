package com.sam.bluepad.data.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

actual class AppDBBuilder(private val context: Context) {

    actual fun getDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(BluePadDB.APP_DB_NAME)
        return Room.databaseBuilder<BluePadDB>(context = appContext, name = dbFile.absolutePath)
            .setDriver(AndroidSQLiteDriver())
    }

    actual fun getMemoryDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        return Room.inMemoryDatabaseBuilder<BluePadDB>(context)
            .setDriver(AndroidSQLiteDriver())
    }
}
