package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.sam.blejavaadvertise.callbacks.GATTServerCallback
import com.sam.blejavaadvertise.models.GATTBluetoothError
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.utils.PlatformInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

private const val TAG = "BLE_ADVERTISEMENT_CALLBACK"

class BLEAdvertisementCallback(
	provider: LocalDeviceInfoProvider,
	private val randomGenerator: RandomGenerator,
	private val platformInfoProvider: PlatformInfoProvider,
) : GATTServerCallback {

	private val _deviceNonceMap = ConcurrentHashMap<String, ByteArray>()

	private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	private val _deviceData = provider.readDeviceInfo
		.stateIn(_scope, SharingStarted.Eagerly, null)

	private val _isRunning = MutableStateFlow(false)
	val isRunning = _isRunning.asStateFlow()

	override fun onServiceAdded(
		serviceUuid: String,
		success: Boolean,
		error: GATTBluetoothError
	) {
		Logger.i(TAG) { "SERVICE $serviceUuid ADDED: ERROR CODE: $error" }
	}

	override fun onServiceStatusChange(status: GATTServiceAdvertisementStatus?) {
		Logger.d(TAG) { "ADVERTISEMENT STATUS :$status" }
		_isRunning.value = status == GATTServiceAdvertisementStatus.STARTED ||
				status == GATTServiceAdvertisementStatus.STARTED_WITHOUT_ADVERTISEMENT
	}

	override fun onReadCharacteristics(
		deviceAddress: String?,
		serviceUuid: String?,
		characteristicUuid: String?,
	): ByteArray? {

		if (serviceUuid == null || characteristicUuid == null || deviceAddress == null) return null

		val characteristicsUUID = Uuid.parse(characteristicUuid)
		val serviceUUID = Uuid.parse(serviceUuid)
		Logger.d(TAG) { "READ REQUESTED ON :$characteristicsUUID SERVICE:$serviceUUID" }

		return when (characteristicsUUID) {
			BLEConstants.connectionNonceCharacteristic -> {
				randomGenerator.generateRandomBytes().also { nonceBytes ->
					_deviceNonceMap[deviceAddress] = nonceBytes
				}
			}

			BLEConstants.deviceIdCharacteristics -> _deviceData.value?.deviceId?.toByteArray()
			BLEConstants.deviceNameCharacteristic -> _deviceData.value?.name?.encodeToByteArray()
			BLEConstants.deviceOSCharacteristics -> platformInfoProvider.platformName()
				.encodeToByteArray()

			BLEConstants.allowSyncCharacteristics -> byteArrayOf(0x1)
			else -> null
		}
	}

	override fun onWriteCharacteristicRequest(
		deviceAddress: String?,
		serviceUuid: String?,
		characteristicUuid: String?,
		value: ByteArray?
	) {
		if (deviceAddress == null || characteristicUuid == null || serviceUuid == null) return

		val characteristicsUUID = Uuid.parse(characteristicUuid)
		val serviceUUID = Uuid.parse(serviceUuid)
		Logger.d(TAG) { "WRITE REQUESTED ON :$characteristicsUUID SERVICE:$serviceUUID" }

		if (serviceUUID != BLEConstants.syncServiceId || value == null) {
			Logger.d(TAG) { "INVALID SERVICE REQUESTED SYNC SERVICE NEEDED OR NO VALUE" }
			return
		}

		val deviceInfo = _deviceData.value ?: run {
			Logger.d(TAG) { "CANNOT READ DEVICE INFO" }
			return
		}

		when (characteristicsUUID) {
			BLEConstants.receiverDeviceIdCharacteristics -> {
				val uuid = try {
					Uuid.fromByteArray(value)
				} catch (_: Exception) {
					Logger.w(TAG) { "CANNOT PASE RECEIVER ID" }
					return
				}
				Logger.d(TAG) { "RECEIVER DEVICE ID FOUND :$uuid" }
			}

			BLEConstants.deviceIdCharacteristics -> {
				val correctAdviser = deviceInfo.deviceId.toByteArray().contentEquals(value)
				Logger.d(TAG) { "SOME ADVISOR FOUND :IS_CORRECT $correctAdviser" }
			}

			BLEConstants.connectionNonceCharacteristic -> {
				val nonceCorrect = _deviceNonceMap[deviceAddress]?.contentEquals(value) ?: false
				Logger.d(TAG) { "CORRECT NONCE , CONNECTION IS UN_HARMED :$nonceCorrect" }
			}

			else -> {}
		}
	}

	fun setRunning(value: Boolean) {
		_isRunning.value = value
	}

	fun cleanUp() {
		if (_scope.isActive) _scope.cancel()
	}

}