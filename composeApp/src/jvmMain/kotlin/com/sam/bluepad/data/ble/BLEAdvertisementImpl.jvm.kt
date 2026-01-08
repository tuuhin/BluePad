package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.sam.ble_common.BluetoothInfoProvider
import com.sam.ble_common.Characteristic
import com.sam.ble_common.Service
import com.sam.blejavaadvertise.BLEAdvertiser
import com.sam.blejavaadvertise.callbacks.GATTServerCallback
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus
import com.sam.blejavaadvertise.models.GattAdvertisementConfig
import com.sam.bluepad.BuildKonfig
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.exceptions.BLEAdvertiseUnsupportedException
import com.sam.bluepad.domain.exceptions.BLENotSupportedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlin.uuid.Uuid

private const val TAG = "BLE_ADVERTISER"

actual class BLEAdvertisementImpl(
	provider: LocalDeviceInfoProvider,
	private val randomGenerator: RandomGenerator,
) : BLEAdvertisementManager {

	private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
	private val _advertiser by lazy { BLEAdvertiser() }

	private val _isRunning = MutableStateFlow(false)

	private val _deviceData = provider.readDeviceInfo
		.stateIn(_scope, SharingStarted.Eagerly, null)

	private val _advertiseCallback = object : GATTServerCallback {
		override fun onServiceAdded(serviceUuid: String, success: Boolean, error: Int) {
			Logger.i(TAG) { "SERVICE $serviceUuid ADDED: ERROR CODE: $error SUCCESS: $success" }
		}

		override fun onServiceStatusChange(status: GATTServiceAdvertisementStatus?) {
			Logger.d(TAG) { "ADVERTISEMENT STATUS :$status" }
			_isRunning.value = status == GATTServiceAdvertisementStatus.STARTED ||
					status == GATTServiceAdvertisementStatus.STARTED_WITHOUT_ADVERTISEMENT
		}

		override fun onReadCharacteristics(serviceUuid: String?, characteristicUuid: String?)
				: ByteArray? {
			if (serviceUuid == null) return null
			if (characteristicUuid == null) return null

			val parsedUUID = Uuid.parse(characteristicUuid)
			Logger.d(TAG) { "READ REQUESTED ON :$parsedUUID" }

			return when (parsedUUID) {
				BLEConstants.deviceNonceCharacteristic -> randomGenerator.generateRandomBytes()
				BLEConstants.deviceIdCharacteristics -> _deviceData.value?.deviceId?.toByteArray()
				BLEConstants.deviceNameCharacteristic -> _deviceData.value?.name?.encodeToByteArray()
				else -> null
			}
		}
	}

	override val isRunning: Flow<Boolean>
		get() = _isRunning

	override val errorFlow: Flow<Exception>
		get() = emptyFlow()

	override suspend fun startAdvertising(): Result<Unit> {

		if (!BluetoothInfoProvider.isBluetoothActive())
			return Result.failure(BluetoothNotEnabledException())

		if (!BLEAdvertiser.nativeIsLeSecureConnectionAvailable())
			return Result.failure(BLENotSupportedException())

		if (!BLEAdvertiser.nativeIsPeripheralRoleSupported())
			return Result.failure(BLEAdvertiseUnsupportedException())

		_advertiser.setListener(_advertiseCallback)
		return try {
			_advertiser.startServer()
			_advertiser.addService(transportService)

			val data = BuildKonfig.APP_ID.encodeToByteArray()
			val config = GattAdvertisementConfig(true, true, data)
			_advertiser.startAdvertisement(config)
			Result.success(Unit)
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	override fun stopAdvertising() {
		_advertiser.stopServer()
		_isRunning.value = false
		Logger.i(TAG) { "ADVERTISEMENT STOPPED" }
	}

	override fun cleanUp() {
		if (_scope.isActive) {
			Logger.d(TAG) { "COROUTINE SCOPE CLEANED" }
		}
		Logger.i(TAG) { "STOPPING GATT SERVER AND CLEANING UP" }
		_advertiser.stopServer()
	}

	companion object {
		private val transportService = Service(
			BLEConstants.transportServiceId.toHexDashString(),
			byteArrayOf(),
			listOf(
				Characteristic(
					BLEConstants.deviceIdCharacteristics.toHexDashString(),
					emptyList(),
					true,
					false,
					false,
					false,
					false
				),
				Characteristic(
					BLEConstants.deviceNameCharacteristic.toHexDashString(),
					emptyList(),
					true,
					false,
					false,
					false, false
				),
				Characteristic(
					BLEConstants.deviceNonceCharacteristic.toHexDashString(),
					emptyList(),
					true,
					false,
					false,
					false, false
				),
			)
		)
	}
}