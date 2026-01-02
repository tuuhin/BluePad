package com.sam.bluepad.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class AppDBBuilder {

	actual fun getDbBuilder(): RoomDatabase.Builder<BluePadDB> {
		val userHome = System.getProperty("user.home")
		val appFolder = File(userHome, "blue_pad").apply { mkdirs() }
		val dbFile = File(appFolder, BluePadDB.APP_DB_NAME)
		if (!dbFile.exists()) dbFile.createNewFile()
		return Room.databaseBuilder<BluePadDB>(name = dbFile.absolutePath)
	}
}