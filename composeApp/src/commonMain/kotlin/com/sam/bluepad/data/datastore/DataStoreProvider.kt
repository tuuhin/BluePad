package com.sam.bluepad.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect class DataStoreProvider {

	fun provideDataStore(fileName: String): DataStore<Preferences>
}