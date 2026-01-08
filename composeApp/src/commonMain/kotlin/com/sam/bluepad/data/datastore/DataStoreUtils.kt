package com.sam.bluepad.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

object DataStoreUtils {

	const val APP_COMMONS_DATASTORE_FILE = "blepad_app_common.preferences_pb"

	// keys
	const val APP_DEVICE_ID_KEY = "blepad_app_device_id"
	const val APP_DEVICE_NAME_KEY = "blepad_app_device_name"

	fun createDataStore(producePath: () -> String): DataStore<Preferences> =
		PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
}