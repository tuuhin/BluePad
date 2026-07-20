package com.sam.bluepad.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.FileSystem
import okio.Path

object DataStoreUtils {

    const val APP_COMMONS_DATASTORE_FILE = "app_properties.pb"
    const val APP_USER_SETTINGS_DATASTORE_FILE = "user_settings.pb"

    // keys
    const val APP_DEVICE_ID_KEY = "blepad_app_device_id"
    const val APP_DEVICE_NAME_KEY = "blepad_app_device_name"

    fun createDataStore(producePath: () -> Path): DataStore<Preferences> =
        DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = producePath,
            ),
        )

    fun <T> createTypedDatastore(
        serializer: OkioSerializer<T>,
        producePath: () -> Path
    ): DataStore<T> = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = serializer,
            producePath = producePath,
        ),
    )
}
