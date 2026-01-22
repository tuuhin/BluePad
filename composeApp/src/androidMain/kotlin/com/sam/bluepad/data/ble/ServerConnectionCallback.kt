package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

private const val TAG = "SERVER_CALLBACK"

private typealias SendResponse = (device: BluetoothDevice?, requestId: Int, status: Int, offset: Int, value: ByteArray?) -> Unit

@SuppressLint("MissingPermission")
class ServerConnectionCallback(
	provider: LocalDeviceInfoProvider,
	private val protoBuf: ProtoBuf,
	private val randomGenerator: RandomGenerator,
	private val platformInfoProvider: PlatformInfoProvider,
) : BluetoothGattServerCallback() {

	private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
	private val _deviceNonceMap = ConcurrentHashMap<String, ByteArray>()

	private val _deviceInfo = provider.readDeviceInfo.stateIn(
		scope = _scope,
		started = SharingStarted.Eagerly,
		initialValue = null
	)

	private var _sendResponse: SendResponse? = null
	private var _onServiceAdded: (() -> Unit)? = null

	fun setOnSendResponse(callback: SendResponse) {
		_sendResponse = callback
	}

	fun setOnServiceAdded(onServiceAdded: () -> Unit = {}) {
		_onServiceAdded = onServiceAdded
	}

	private val _syncDevices = MutableStateFlow<List<RemoteDeviceResponse>>(emptyList())
	val remoteSyncDevice: Flow<List<Uuid>> = _syncDevices.mapNotNull { devices ->
		devices.filter { it.isAllValid }.mapNotNull { it.removeDeviceId }
	}

	private val _externalPeer = MutableStateFlow<List<BLEPeerData>>(emptyList())
	val externalPeers = _externalPeer.asStateFlow()

	override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
		if (device == null || status != BluetoothGatt.GATT_SUCCESS) return
		if (newState == BluetoothProfile.STATE_CONNECTED) {
			val newDevice = RemoteDeviceResponse(device.address)
			_syncDevices.update { devices -> (devices + newDevice).distinctBy { it.address } }
		} else {
			_syncDevices.update { devices -> (devices.filterNot { it.address == device.address }) }
		}
		val bondState = when (device.bondState) {
			BluetoothDevice.BOND_BONDED -> "BONDED"
			BluetoothDevice.BOND_BONDING -> "BONDING"
			BluetoothDevice.BOND_NONE -> "NO BOND"
			else -> null
		}
		Logger.d(TAG) { "DEVICE IDENTIFIER:${device.address} BOND STATE: $bondState CONNECTION STATE CHANGED" }
	}

	override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Logger.w(TAG) { "SOME ERROR IN ADDING THE SERVICE: $status" }
			return
		}
		if (service == null) return
		Logger.i(TAG) { "SERVICE :${service.uuid} ADDED" }
		_onServiceAdded?.invoke()
	}

	override fun onCharacteristicReadRequest(
		device: BluetoothDevice?,
		requestId: Int,
		offset: Int,
		characteristic: BluetoothGattCharacteristic?
	) {
		if (device == null || characteristic == null) {
			sendFailedResponse(device, requestId, offset)
			return
		}
		// handle discovery service here only
		if (characteristic.service.uuid.toKotlinUuid() == BLEConstants.discoveryServiceId) {
			Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM DISCOVERY SERVICE" }

			val deviceInfo = _deviceInfo.value ?: run {
				Logger.e(TAG) { "CANNOT READ DEVICE INFO" }
				sendFailedResponse(device, requestId, offset)
				return
			}

			val value = when (characteristic.uuid.toKotlinUuid()) {
				BLEConstants.deviceInfoCharacteristics -> {
					try {
						val nonce = randomGenerator.generateRandomBytes(12).also { nonceBytes ->
							_deviceNonceMap[device.address] = nonceBytes
						}
						val peerData = BLEPeerData(
							deviceId = deviceInfo.deviceId,
							deviceName = deviceInfo.name,
							nonce = nonce.decodeToString(),
							deviceOs = platformInfoProvider.platformOS,
						)
						protoBuf.encodeToByteArray<BLEPeerData>(peerData)

					} catch (e: Exception) {
						Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
						sendFailedResponse(device, requestId, offset)
						return
					}
				}

				else -> {
					sendFailedResponse(device, requestId, offset)
					return
				}
			}
			Logger.d(TAG) { "SENDING SUCCESS READ RESPONSE FOR CHARACTERISTICS :${characteristic.uuid}" }
			_sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
			return
		}

		// handle the sync service here
		if (characteristic.service.uuid.toKotlinUuid() == BLEConstants.syncServiceId) {
			Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }
			// TODO: FILL THE REQUIREMENTS
			_sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
			return
		}
		// invalids
		Logger.w(TAG) { "REQUESTING INVALID SERVICE" }
		sendFailedResponse(device, requestId, offset)
	}

	override fun onCharacteristicWriteRequest(
		device: BluetoothDevice?,
		requestId: Int,
		characteristic: BluetoothGattCharacteristic?,
		preparedWrite: Boolean,
		responseNeeded: Boolean,
		offset: Int,
		value: ByteArray?
	) {
		// ensure we have some data
		if (device == null || characteristic == null || value == null) {
			sendFailedResponse(device, requestId, offset, responseNeeded)
			return
		}

		if (characteristic.service.uuid.toKotlinUuid() == BLEConstants.discoveryServiceId) {
			Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM DISCOVERY SERVICE" }

			when (characteristic.uuid.toKotlinUuid()) {
				BLEConstants.deviceInfoCharacteristics -> {
					try {
						val peerData = protoBuf.decodeFromByteArray<BLEPeerData>(value)
						_externalPeer.update { devices -> (devices + peerData).distinctBy { it.deviceId } }
					} catch (e: Exception) {
						Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
						sendFailedResponse(device, requestId, offset, responseNeeded)
						return
					}
				}

				else -> {
					sendFailedResponse(device, requestId, offset, responseNeeded)
					return
				}
			}
			if (!responseNeeded) return
			Logger.d(TAG) { "SENDING SUCCESS WRITE RESPONSE FOR CHARACTERISTICS :${characteristic.uuid}" }
			_sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
			return
		}
		// sync service
		if (characteristic.service.uuid.toKotlinUuid() == BLEConstants.syncServiceId) {
			Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }
			sendFailedResponse(device, requestId, offset, responseNeeded)
			return
		}
		// invalids
		Logger.w(TAG) { "REQUESTING INVALID SERVICE" }
		sendFailedResponse(device, requestId, offset, responseNeeded)
	}

	private fun sendFailedResponse(
		device: BluetoothDevice?,
		requestId: Int,
		offset: Int,
		responseNeeded: Boolean = true
	) {
		if (!responseNeeded) return
		_sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
	}


	fun cleanUp() {
		// clears everything on done
		_scope.cancel()
		_deviceNonceMap.clear()
		_syncDevices.value = emptyList()
		_externalPeer.value = emptyList()
	}
}

private data class RemoteDeviceResponse(
	val address: String,
	val currentDeviceIdValidated: Boolean = false,
	val readNonceValidated: Boolean = false,
	val removeDeviceId: Uuid? = null,
) {
	val isAllValid: Boolean
		get() = removeDeviceId != null && currentDeviceIdValidated && readNonceValidated
}
