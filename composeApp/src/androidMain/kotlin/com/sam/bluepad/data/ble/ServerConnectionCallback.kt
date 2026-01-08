package com.sam.bluepad.data.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

private const val TAG = "SERVER_CALLBACK"

private typealias SendResponse = (device: BluetoothDevice, requestId: Int, status: Int, offset: Int, value: ByteArray?) -> Unit

class ServerConnectionCallback(
	provider: LocalDeviceInfoProvider,
	private val randomGenerator: RandomGenerator
) : BluetoothGattServerCallback() {

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
		if (device == null || characteristic == null) return
		Logger.d(TAG) { "READ REQUESTED FOR CHARACTERISTICS :${characteristic.uuid}" }
		if (characteristic.service.uuid.toKotlinUuid() != BLEConstants.transportServiceId) {
			Logger.d(TAG) { "INVALID SERVICE REQUESTED" }
			_sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
			return
		}

		val deviceInfo = deviceInfo.value ?: run {
			Logger.d(TAG) { "CANNOT READ DEVICE INFO" }
			_sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
			return
		}
		val value = when (characteristic.uuid) {
			BLEConstants.deviceIdCharacteristics.toJavaUuid() -> deviceInfo.deviceId.toByteArray()
			BLEConstants.deviceNameCharacteristic.toJavaUuid() -> deviceInfo.name.encodeToByteArray()
			BLEConstants.deviceNonceCharacteristic.toJavaUuid() -> randomGenerator.generateRandomBytes()
			else -> null
		}
		val isSuccess = if (value == null) BluetoothGatt.GATT_FAILURE
		else BluetoothGatt.GATT_SUCCESS
		Logger.d(TAG) { "SENDING RESPONSE FOR CHARACTERISTICS :${characteristic.uuid}" }
		_sendResponse?.invoke(device, requestId, isSuccess, offset, value)
	}

	fun cleanUp() = _scope.cancel()
}