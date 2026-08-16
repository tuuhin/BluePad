package com.sam.bluepad.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.preferences.core.Preferences
import com.sam.bluepad.data.utils.CommonAppFilesStore

class DataStoreProvider(private val storage: CommonAppFilesStore) {

    fun providePreferencesDataStore(fileName: String): DataStore<Preferences> = DataStoreUtils.createDataStore(
        producePath = { storage.filesDirectory() / "settings" / fileName },
    )

    fun <T> provideSettingsDataStore(
        fileName: String,
        serializer: OkioSerializer<T>
    ): DataStore<T> = DataStoreUtils.createTypedDatastore(
        serializer = serializer,
        producePath = { storage.filesDirectory() / "settings" / fileName },
    )
}
