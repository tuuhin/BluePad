package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.utils.PlatformInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

private const val TAG = "SERVER_CALLBACK"

private typealias SendResponse = (device: BluetoothDevice?, requestId: Int, status: Int, offset: Int, value: ByteArray?) -> Unit

@SuppressLint("MissingPermission")
class ServerConnectionCallback(
	provider: LocalDeviceInfoProvider,
	private val randomGenerator: RandomGenerator,
	private val platformInfoProvider: PlatformInfoProvider,
) : BluetoothGattServerCallback() {

	private val _deviceNonceMap = ConcurrentHashMap<String, ByteArray>()
	private val _syncDevices = MutableStateFlow<List<RemoteDeviceResponse>>(emptyList())

	private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	private val deviceInfo = provider.readDeviceInfo.stateIn(
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

	val remoteSyncDevice: Flow<List<Uuid>> = _syncDevices.mapNotNull { devices ->
		devices.filter { it.isAllValid }.mapNotNull { it.removeDeviceId }
	}

	override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
		if (device == null || status != BluetoothGatt.GATT_SUCCESS) return
		if (newState == BluetoothProfile.STATE_CONNECTED) {
			val newDevice = RemoteDeviceResponse(device.address)
			_syncDevices.update { devices -> (devices + newDevice).distinctBy { it.address } }
		} else {
			_syncDevices.update { devices -> (devices.filterNot { it.address == device.address }) }
		}
		Logger.d(TAG) { "DEVICE IDENTIFIER:${device.address} BOND STATE: ${device.bondState} CONNECTION STATE CHANGED" }
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
		Logger.d(TAG) { "READ REQUESTED FOR CHARACTERISTICS :${characteristic.uuid} SERVICE: ${characteristic.service.uuid}" }

		if (characteristic.service.uuid.toKotlinUuid() != BLEConstants.transportServiceId || characteristic.service.uuid.toKotlinUuid() != BLEConstants.syncServiceId) {
			Logger.d(TAG) { "INVALID SERVICE REQUESTED" }
			sendFailedResponse(device, requestId, offset)
			return
		}

		val deviceInfo = deviceInfo.value ?: run {
			Logger.d(TAG) { "CANNOT READ DEVICE INFO" }
			sendFailedResponse(device, requestId, offset)
			return
		}
		val value = when (characteristic.uuid.toKotlinUuid()) {
			BLEConstants.deviceIdCharacteristics -> deviceInfo.deviceId.toByteArray()
			BLEConstants.deviceNameCharacteristic -> deviceInfo.name.encodeToByteArray()
			BLEConstants.deviceOSCharacteristics -> platformInfoProvider.platformName()
				.encodeToByteArray()

			BLEConstants.allowSyncCharacteristics -> byteArrayOf(0x1)
			BLEConstants.connectionNonceCharacteristic -> randomGenerator.generateRandomBytes(12)
				.also { nonceBytes ->
					_deviceNonceMap[device.address] = nonceBytes
				}

			else -> {
				sendFailedResponse(device, requestId, offset)
				return
			}
		}
		Logger.d(TAG) { "SENDING SUCCESS RESPONSE FOR CHARACTERISTICS :${characteristic.uuid}" }
		_sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
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
		if (device == null || characteristic == null) {
			sendFailedResponse(device, requestId, offset, responseNeeded)
			return
		}
		Logger.d(TAG) { "READ REQUESTED FOR CHARACTERISTICS :${characteristic.uuid} SERVICE: ${characteristic.service.uuid}" }

		if (characteristic.service.uuid.toKotlinUuid() != BLEConstants.syncServiceId || value == null) {
			Logger.d(TAG) { "INVALID SERVICE REQUESTED SYNC SERVICE NEEDED OR NO VALUE" }
			sendFailedResponse(device, requestId, offset, responseNeeded)
			return
		}

		val deviceInfo = deviceInfo.value ?: run {
			Logger.d(TAG) { "CANNOT READ DEVICE INFO" }
			sendFailedResponse(device, requestId, offset, responseNeeded)
			return
		}

		when (characteristic.uuid.toKotlinUuid()) {
			BLEConstants.receiverDeviceIdCharacteristics -> {
				val uuid = try {
					Uuid.fromByteArray(value)
				} catch (_: Exception) {
					Logger.w(TAG) { "CANNOT PASE RECEIVER ID" }
					sendFailedResponse(device, requestId, offset, responseNeeded)
					return
				}
				Logger.d(TAG) { "RECEIVER DEVICE ID FOUND :$uuid" }
				_syncDevices.update { devices ->
					devices.map { dv ->
						if (dv.address == device.address) dv.copy(removeDeviceId = uuid)
						else dv
					}
				}
			}

			BLEConstants.deviceIdCharacteristics -> {
				val correctAdviser = deviceInfo.deviceId.toByteArray().contentEquals(value)
				Logger.d(TAG) { "SOME ADVISOR FOUND :IS_CORRECT $correctAdviser" }
				_syncDevices.update { devices ->
					devices.map { dv ->
						if (dv.address == device.address) dv.copy(currentDeviceIdValidated = correctAdviser)
						else dv
					}
				}
				if (!correctAdviser) {
					sendFailedResponse(device, requestId, offset, responseNeeded)
					return
				}
			}

			BLEConstants.connectionNonceCharacteristic -> {
				val nonceCorrect = _deviceNonceMap[device.address]?.contentEquals(value) ?: false
				_syncDevices.update { devices ->
					devices.map { dv ->
						if (dv.address == device.address) dv.copy(readNonceValidated = nonceCorrect)
						else dv
					}
				}
				Logger.d(TAG) { "CORRECT NONCE , CONNECTION IS UN_HARMED" }
				if (!nonceCorrect) {
					sendFailedResponse(device, requestId, offset, responseNeeded)
					return
				}
			}

			else -> {}
		}

		if (!responseNeeded) return
		// finally a success message
		_sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
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
