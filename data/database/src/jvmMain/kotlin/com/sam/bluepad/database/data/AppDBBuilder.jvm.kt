package com.sam.bluepad.database.data

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sam.bluepad.common.utils.IFilesProvider
import org.koin.core.annotation.Singleton

@Singleton
actual class AppDBBuilder(
    private val filesStore: IFilesProvider
) {


    actual fun getDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        val userHome = filesStore.filesDirectory()
        val dbFile = (userHome / BluePadDB.APP_DB_NAME).toFile()
        if (!dbFile.exists()) dbFile.createNewFile()
        return Room
            .databaseBuilder<BluePadDB>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
    }

    actual fun getMemoryDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        val userHome = filesStore.filesDirectory()
        val dbFile = (userHome / BluePadDB.APP_DB_NAME).toFile()
        if (!dbFile.exists()) dbFile.createNewFile()
        return Room
            .databaseBuilder<BluePadDB>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
    }
}
