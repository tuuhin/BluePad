package com.sam.bluepad.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

actual class DataStoreProvider {

	actual fun provideDataStore(fileName: String): DataStore<Preferences> =
		DataStoreUtils.createDataStore(
			producePath = {
				val userHome = System.getProperty("user.home")
				val appFolder = File(userHome, "blue_pad").apply { mkdirs() }
				val file = File(appFolder, fileName)
				file.absolutePath
			}
		)
}