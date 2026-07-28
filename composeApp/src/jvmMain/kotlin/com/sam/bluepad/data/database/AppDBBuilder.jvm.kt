package com.sam.bluepad.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.sam.bluepad.data.utils.CommonAppFilesStore

actual class AppDBBuilder(private val filesStore: CommonAppFilesStore) {

    actual fun getDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        val userHome = filesStore.filesDirectory()
        val dbFile = (userHome / BluePadDB.APP_DB_NAME).toFile()
        if (!dbFile.exists()) dbFile.createNewFile()
        return Room.databaseBuilder<BluePadDB>(name = dbFile.absolutePath)
    }

    actual fun getMemoryDbBuilder(): RoomDatabase.Builder<BluePadDB> {
        return Room.inMemoryDatabaseBuilder()
    }
}
