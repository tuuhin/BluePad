package com.sam.bluepad.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomNameGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

private const val TAG = "LOCAL_DEVICE_INFO_PROVIDER"

class LocalDeviceInfoProviderImpl(
	private val preferences: DataStore<Preferences>,
	private val randomNameGenerator: RandomNameGenerator,
) : LocalDeviceInfoProvider {

	private val _deviceIdKey = byteArrayPreferencesKey(DataStoreUtils.APP_DEVICE_ID_KEY)
	private val _deviceName = stringPreferencesKey(DataStoreUtils.APP_DEVICE_NAME_KEY)

	override val readDeviceId: Flow<Uuid>
		get() = preferences.data
			.catch { err -> Logger.w(TAG, err) { "PREFERENCES ERROR" } }
			.map { prefs ->
				prefs[_deviceIdKey]?.let(Uuid::fromByteArray) ?: Uuid.NIL
			}

	override val readDeviceInfo: Flow<LocalDeviceInfoModel>
		get() = preferences.data
			.catch { err -> Logger.w(TAG, err) { "PREFERENCES ERROR" } }
			.map { prefs ->
				val deviceName = prefs[_deviceName] ?: ""
				val deviceId = prefs[_deviceIdKey]?.let(Uuid::fromByteArray) ?: Uuid.NIL
				LocalDeviceInfoModel(deviceId = deviceId, deviceName)
			}

	override suspend fun initiateDeviceInfo() {
		preferences.edit { prefs ->
			if (!prefs.contains(_deviceName)) {
				prefs[_deviceName] = randomNameGenerator.generateName()
				Logger.d(TAG) { "DEVICE NAME ADDED" }
			}

			if (!prefs.contains(_deviceIdKey)) {
				val randomId = Uuid.random()
				prefs[_deviceIdKey] = randomId.toByteArray()
				Logger.d(TAG) { "DEVICE ID ADDED :$randomId" }
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