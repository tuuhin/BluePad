package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.ble.BLEConstants
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

private const val TAG = "BLEScanPeerCallback"

@SuppressLint("MissingPermission")
class ConnectionCallback(
	private val onReadCharacteristic: (BluetoothGatt, Uuid, String) -> Unit
) : BluetoothGattCallback() {

	override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Logger.e(TAG) { "CANNOT DISCOVER SERVICES" }
			return
		}
		when (newState) {
			BluetoothGatt.STATE_CONNECTED -> {
				Logger.d(TAG) { "DEVICE CONNECTED" }
				//discover services
				gatt?.discoverServices()
			}

			BluetoothGatt.STATE_DISCONNECTED -> {
				Logger.d(TAG) { "DEVICE DISCONNECTED" }
				gatt?.close()
			}

			else -> Logger.d(TAG) { "DEVICE STATE NOT CONNECTED : $newState" }
		}
	}

	override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Logger.e(TAG) { "CANNOT DISCOVER SERVICES" }
			return
		}
		val service = gatt?.services?.find { it.uuid == BLEConstants.transportServiceId } ?: run {
			gatt?.disconnect()
			gatt?.close()
			return
		}
		Logger.d(TAG) { "CORRECT GATT SERVICE FOUND" }
		// we read
		val requiredCharacteristics = service.characteristics
			.filter { it.uuid == BLEConstants.deviceNameCharacteristic || it.uuid == BLEConstants.deviceNonceCharacteristic }

		if (requiredCharacteristics.isEmpty()) {
			gatt.disconnect()
			gatt.close()
			return
		}
		requiredCharacteristics.forEach { gatt.readCharacteristic(it) }
	}

	override fun onCharacteristicRead(
		gatt: BluetoothGatt,
		characteristic: BluetoothGattCharacteristic,
		value: ByteArray,
		status: Int
	) {
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Logger.e(TAG) { "CANNOT READ CHARACTERISTICS" }
			return
		}
		val valueAsString = value.decodeToString()
		onReadCharacteristic(gatt, characteristic.uuid.toKotlinUuid(), valueAsString)
	}
}