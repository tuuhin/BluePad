package com.sam.bluepad.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sam.bluepad.data.utils.CommonAppFilesStore

actual class AppDBBuilder(private val filesStore: CommonAppFilesStore) {

    actual fun getDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        val userHome = filesStore.filesDirectory()
        val dbFile = (userHome / BluePadDB.APP_DB_NAME).toFile()
        if (!dbFile.exists()) dbFile.createNewFile()
        return Room.databaseBuilder<BluePadDB>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
    }

    actual fun getMemoryDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        return Room.inMemoryDatabaseBuilder<BluePadDB>()
            .setDriver(BundledSQLiteDriver())

    }
}
