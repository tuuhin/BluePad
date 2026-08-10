package com.sam.preferences.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.sam.bluepad.common.shared.RandomNameGenerator
import com.sam.bluepad.common.utils.IPlatformDataProvider
import com.sam.bluepad.domain.repository.ILocalDeviceRepository
import com.sam.bluepad.model.devices.LocalDeviceModel
import com.sam.preferences.data.datastore.DataStoreCommon
import com.sam.preferences.data.datastore.DataStoreProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

private const val TAG = "LOCAL_DEVICE_DATA"

@Singleton(binds = [ILocalDeviceRepository::class])
internal class LocalDeviceDataRepo(
    private val datastore: DataStoreProvider,
    private val platformProvider: IPlatformDataProvider,
    private val randomNameGenerator: RandomNameGenerator,
) : ILocalDeviceRepository {


    private val preferences: DataStore<Preferences> by lazy {
        datastore.providePreferencesDataStore(DataStoreCommon.APP_COMMONS_DATASTORE_FILE)
    }

    private val _deviceIdKey = byteArrayPreferencesKey(DataStoreCommon.APP_DEVICE_ID_KEY)
    private val _deviceName = stringPreferencesKey(DataStoreCommon.APP_DEVICE_NAME_KEY)


    override val readDeviceInfo: Flow<LocalDeviceModel>
        get() = preferences.data
            .catch { err -> Logger.w(tag = TAG, throwable = err) { "PREFERENCES ERROR" } }
            .map { prefs ->
                val deviceName = prefs[_deviceName] ?: ""
                val deviceId = prefs[_deviceIdKey]?.let(Uuid::fromByteArray) ?: Uuid.NIL
                LocalDeviceModel(uuid = deviceId, aliasName = deviceName, platformProvider.platformOS)
            }

    override suspend fun initiateDeviceInfo() {
        preferences.edit { prefs ->
            if (!prefs.contains(_deviceName)) {
                prefs[_deviceName] = randomNameGenerator.generateName()
                Logger.d(tag = TAG) { "DEVICE NAME ADDED" }
            }

            if (!prefs.contains(_deviceIdKey)) {
                val randomId = Uuid.random()
                prefs[_deviceIdKey] = randomId.toByteArray()
                Logger.d(tag = TAG) { "DEVICE ID ADDED :$randomId" }
            }
        }

    }

    override suspend fun updateDeviceId(): Uuid {
        val randomId = Uuid.random()
        preferences.edit { prefs ->
            prefs[_deviceIdKey] = randomId.toByteArray()
        }
        return randomId
    }

    override suspend fun updateDeviceName(newName: String) {
        preferences.edit { prefs -> prefs[_deviceName] = newName }
    }
}
