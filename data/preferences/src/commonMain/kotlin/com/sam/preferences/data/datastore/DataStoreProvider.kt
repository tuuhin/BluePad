package com.sam.preferences.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sam.bluepad.common.utils.IFilesProvider
import org.koin.core.annotation.Singleton

@Singleton
internal class DataStoreProvider(
    private val storage: IFilesProvider
) {


    fun providePreferencesDataStore(fileName: String): DataStore<Preferences> = DataStoreCommon.createDataStore(
        producePath = { storage.filesDirectory() / "settings" / fileName },
    )

    fun provideSettingsDataStore(fileName: String): DataStore<UserAppSettingsKT> = DataStoreCommon.createTypedDatastore(
        serializer = AppSettingsSerializer,
        producePath = { storage.filesDirectory() / "settings" / fileName },
    )
}
