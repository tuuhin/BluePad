package com.sam.bluepad.data.ble.utils

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import androidx.annotation.RequiresPermission
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.ble.BLEConstants
import kotlin.uuid.toJavaUuid

private const val TAG = "BT_GATT_EXTENSIONS"

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Suppress("DEPRECATION")
fun BluetoothGatt.writeToCharacteristics(
	characteristic: BluetoothGattCharacteristic,
	value: ByteArray,
	writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
): Boolean {

	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		val status = writeCharacteristic(characteristic, value, writeType)
		if (status != 0) Logger.w(TAG) { "GATT WRITE FAILED REASON_CODE:$status" }
		status == BluetoothStatusCodes.SUCCESS
	} else {
		characteristic.value = value
		writeCharacteristic(characteristic)
	}
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Suppress("DEPRECATION")
fun BluetoothGatt.writeToDescriptor(
	descriptor: BluetoothGattDescriptor,
	value: ByteArray,
): Boolean {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		val status = writeDescriptor(descriptor, value)
		if (status != BluetoothStatusCodes.SUCCESS) {
			Logger.w(TAG) { "FAILED TO WRITE TO DESCRIPTOR REASON_CODE:$status" }
		}
		status == BluetoothStatusCodes.SUCCESS
	} else {
		descriptor.value = value
		writeDescriptor(descriptor)
	}
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Suppress("DEPRECATION")
fun BluetoothGatt.toggleNotification(
	characteristic: BluetoothGattCharacteristic,
	isEnabled: Boolean,
): Boolean {
	// write enable to descriptor
	val gattValue = when (isEnabled) {
		true if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
		true -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
		false -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
	}

	val cccDescriptor = characteristic.getDescriptor(BLEConstants.CCC_DESCRIPTOR.toJavaUuid())
		?: return false

	val isWriteSuccess = writeToDescriptor(cccDescriptor, gattValue)
	val valueAsString = if (isEnabled) "ENABLED" else "DISABLED"
	Logger.d(TAG) { "WRITING CCC DESCRIPTOR VALUE $valueAsString IS_SUCCESS:$isWriteSuccess" }

	val isSetSuccessfully = setCharacteristicNotification(characteristic, isEnabled)
	Logger.d(TAG) { "NOTIFICATION ENABLED:$isEnabled: CHARACTERISTICS:${characteristic.uuid} IS_SET:$isSetSuccessfully" }
	return isSetSuccessfully && isWriteSuccess
}

val BluetoothGattCharacteristic.hasIndication: Boolean
	get() = properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0


val ByteArray.btDescriptorsNotificationOrIndicationEnabled: Boolean
	get() = when {
		contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) -> true
		contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) -> true
		contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) -> false
		else -> throw IllegalArgumentException("Invalid value")
	}
