package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.sam.blejavaadvertise.callbacks.GATTServerCallback
import com.sam.blejavaadvertise.models.GATTBluetoothError
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

private const val TAG = "BLE_ADVERTISEMENT_CALLBACK"

class BLEAdvertisementCallback(
	localDeviceInfo: LocalDeviceInfoProvider,
	private val protoBuf: ProtoBuf,
	private val randomGenerator: RandomGenerator,
	private val platformInfoProvider: PlatformInfoProvider,
) : GATTServerCallback {

	private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
	private val _deviceNonceMap = ConcurrentHashMap<String, ByteArray>()

	private val _deviceInfo = localDeviceInfo.readDeviceInfo
		.stateIn(_scope, SharingStarted.Eagerly, null)

	private val _isRunning = MutableStateFlow(false)
	val isRunning = _isRunning.asStateFlow()

	private val _externalPeer = MutableStateFlow<List<BLEPeerData>>(emptyList())
	val externalPeers = _externalPeer.asStateFlow()

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

		if (serviceUuid == null || characteristicUuid == null || deviceAddress == null)
			return null

		val characteristicsUUID = Uuid.parse(characteristicUuid)
		val serviceUUID = Uuid.parse(serviceUuid)

		if (serviceUUID == BLEConstants.discoveryServiceId) {
			Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : $characteristicsUUID FROM DISCOVERY SERVICE" }

			val deviceInfo = _deviceInfo.value ?: return null

			val value = when (characteristicsUUID) {
				BLEConstants.deviceInfoCharacteristics -> {
					val nonce = randomGenerator.generateRandomBytes(12).also { nonceBytes ->
						_deviceNonceMap[deviceAddress] = nonceBytes
					}
					val peerData = BLEPeerData(
						deviceId = deviceInfo.deviceId,
						deviceName = deviceInfo.name,
						nonce = nonce.decodeToString(),
						deviceOs = platformInfoProvider.platformOS,
					)
					protoBuf.encodeToByteArray<BLEPeerData>(peerData)
				}

				else -> null
			}
			Logger.d(TAG) { "SENDING SUCCESS RESPONSE FOR CHARACTERISTICS :${characteristicUuid}" }
			return value
		}
		// handle the sync service here
		if (serviceUUID == BLEConstants.syncServiceId) {
			Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : $characteristicUuid FROM SYNC SERVICE" }
			// TODO: FILL THE REQUIREMENTS
			return null
		}
		Logger.w(TAG) { "REQUESTING INVALID SERVICE" }
		return null
	}

	override fun onWriteCharacteristicRequest(
		deviceAddress: String?,
		serviceUuid: String?,
		characteristicUuid: String?,
		value: ByteArray?
	) {
		if (deviceAddress == null || characteristicUuid == null || serviceUuid == null || value == null)
			return

		val characteristicsUUID = Uuid.parse(characteristicUuid)
		val serviceUUID = Uuid.parse(serviceUuid)
		Logger.d(TAG) { "WRITE REQUESTED ON :$characteristicsUUID SERVICE:$serviceUUID" }

		if (serviceUUID == BLEConstants.discoveryServiceId) {
			Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicsUUID FROM DISCOVERY SERVICE" }

			when (characteristicsUUID) {
				BLEConstants.deviceInfoCharacteristics -> {
					val peerData = try {
						protoBuf.decodeFromByteArray<BLEPeerData>(value)
					} catch (e: Exception) {
						Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
						return
					}
					_externalPeer.update { devices -> (devices + peerData).distinctBy { it.deviceId } }
				}

				else -> return
			}
		}
		if (serviceUUID == BLEConstants.syncServiceId) {
			Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicUuid FROM SYNC SERVICE" }
			// TODO: FILL THE REQUIREMENTS
			return
		}
		// invalids
		Logger.w(TAG) { "REQUESTING INVALID SERVICE" }
	}

	fun setRunning(value: Boolean) {
		_isRunning.value = value
	}

	fun cleanUp() {
		if (_scope.isActive) _scope.cancel()
		_externalPeer.update { emptyList() }
		_deviceNonceMap.clear()
	}

}