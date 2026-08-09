package com.sam.bluepad.database.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import org.koin.core.annotation.Singleton

@Singleton
actual class AppDBBuilder(
    private val context: Context
) {


    actual fun getDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(BluePadDB.APP_DB_NAME)
        return Room
            .databaseBuilder<BluePadDB>(
                context = appContext,
                name = dbFile.absolutePath,
            ).setDriver(AndroidSQLiteDriver())
    }

    actual fun getMemoryDbBuilder(): RoomDatabase.Builder<BluePadDB> =
        Room
            .inMemoryDatabaseBuilder<BluePadDB>(context)
            .setDriver(AndroidSQLiteDriver())
}
