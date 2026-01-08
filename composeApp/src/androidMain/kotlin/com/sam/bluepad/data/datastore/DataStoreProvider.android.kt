package com.sam.bluepad.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

actual class DataStoreProvider(private val context: Context) {
	actual fun provideDataStore(fileName: String): DataStore<Preferences> =
		DataStoreUtils.createDataStore(
			producePath = { context.filesDir.resolve(fileName).absolutePath }
		)
}