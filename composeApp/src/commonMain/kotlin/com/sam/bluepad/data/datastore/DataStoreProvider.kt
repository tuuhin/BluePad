package com.sam.bluepad.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sam.bluepad.data.datastore.serializers.AppSettingsSerializer
import com.sam.bluepad.data.datastore.serializers.UserAppSettingsKT
import com.sam.bluepad.data.utils.CommonAppFilesStore

class DataStoreProvider(private val storage: CommonAppFilesStore) {

    fun providePreferencesDataStore(fileName: String): DataStore<Preferences> = DataStoreUtils.createDataStore(
        producePath = { storage.filesDirectory() / "settings" / fileName },
    )

    fun provideSettingsDataStore(fileName: String): DataStore<UserAppSettingsKT> = DataStoreUtils.createTypedDatastore(
        serializer = AppSettingsSerializer,
        producePath = { storage.filesDirectory() / "settings" / fileName },
    )
}
