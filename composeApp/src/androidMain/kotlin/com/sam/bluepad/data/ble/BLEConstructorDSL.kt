package com.sam.bluepad.data.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.sam.bluepad.domain.ble.BLEPermission
import com.sam.bluepad.domain.ble.BLEPropertyType
import com.sam.bluepad.domain.ble.BLEServiceType
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

fun bleServiceOf(
	uuid: Uuid,
	serviceType: BLEServiceType,
	characteristics: List<BluetoothGattCharacteristic> = emptyList(),
): BluetoothGattService =
	BluetoothGattService(
		uuid.toJavaUuid(),
		when (serviceType) {
			BLEServiceType.PRIMARY -> BluetoothGattService.SERVICE_TYPE_PRIMARY
			BLEServiceType.SECONDARY -> BluetoothGattService.SERVICE_TYPE_SECONDARY
		}
	).apply {
		characteristics.forEach { addCharacteristic(it) }
	}

fun bleCharacteristicsOf(
	uuid: Uuid,
	properties: List<BLEPropertyType>,
	permissions: List<BLEPermission>,
): BluetoothGattCharacteristic {

	require(properties.isNotEmpty() || properties.contains(BLEPropertyType.UNKNOWN)) {
		"Required at-least a single property and the use of property unknown cannot be used"
	}

	require(permissions.isNotEmpty() || permissions.contains(BLEPermission.PERMISSION_UNKNOWN)) {
		"Required at-least a single permission associated with the properties"
	}

	var gattProperty = 0
	var gattPermission = 0
	properties.filterNot { it == BLEPropertyType.UNKNOWN }.forEach { type ->
		gattProperty = when (type) {
			BLEPropertyType.PROPERTY_BROADCAST -> gattProperty or BluetoothGattCharacteristic.PROPERTY_BROADCAST
			BLEPropertyType.PROPERTY_READ -> gattProperty or BluetoothGattCharacteristic.PROPERTY_READ
			BLEPropertyType.PROPERTY_WRITE_NO_RESPONSE -> gattProperty or BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
			BLEPropertyType.PROPERTY_WRITE -> gattProperty or BluetoothGattCharacteristic.PROPERTY_WRITE
			BLEPropertyType.PROPERTY_NOTIFY -> gattProperty or BluetoothGattCharacteristic.PROPERTY_NOTIFY
			BLEPropertyType.PROPERTY_INDICATE -> gattProperty or BluetoothGattCharacteristic.PROPERTY_INDICATE
			BLEPropertyType.PROPERTY_SIGNED_WRITE -> gattProperty or BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
			BLEPropertyType.PROPERTY_EXTENDED_PROPS -> gattProperty or BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS
			BLEPropertyType.UNKNOWN -> gattProperty
		}
	}

	permissions.filterNot { it == BLEPermission.PERMISSION_UNKNOWN }.forEach { perms ->
		gattPermission = when (perms) {
			BLEPermission.PERMISSION_READ -> gattPermission or BluetoothGattCharacteristic.PERMISSION_READ
			BLEPermission.PERMISSION_READ_ENCRYPTED -> gattPermission or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
			BLEPermission.PERMISSION_READ_ENCRYPTED_MITM -> gattPermission or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
			BLEPermission.PERMISSION_WRITE -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE
			BLEPermission.PERMISSION_WRITE_ENCRYPTED -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
			BLEPermission.PERMISSION_WRITE_ENCRYPTED_MITM -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
			BLEPermission.PERMISSION_WRITE_SIGNED -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED
			BLEPermission.PERMISSION_WRITE_SIGNED_MITM -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM
			BLEPermission.PERMISSION_UNKNOWN -> gattPermission
		}
	}

	val writeProperties = listOf(
		BLEPropertyType.PROPERTY_WRITE, BLEPropertyType.PROPERTY_SIGNED_WRITE,
		BLEPropertyType.PROPERTY_WRITE_NO_RESPONSE
	)

	return BluetoothGattCharacteristic(uuid.toJavaUuid(), gattProperty, gattPermission).apply {
		if (properties.containsAll(writeProperties))
			writeType = when {
				BLEPropertyType.PROPERTY_WRITE in properties -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
				BLEPropertyType.PROPERTY_WRITE_NO_RESPONSE in properties -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
				BLEPropertyType.PROPERTY_SIGNED_WRITE in properties -> BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
				else -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
			}
	}
}

fun bleDescriptorOf(uuid: UUID, permissions: List<BLEPermission>): BluetoothGattDescriptor {

	require(permissions.isNotEmpty() || permissions.contains(BLEPermission.PERMISSION_UNKNOWN)) {
		"Required at-least a single permission associated with the properties"
	}

	var gattPermission = 0

	permissions.filterNot { it == BLEPermission.PERMISSION_UNKNOWN }.forEach { perms ->
		gattPermission = when (perms) {
			BLEPermission.PERMISSION_READ -> gattPermission or BluetoothGattCharacteristic.PERMISSION_READ
			BLEPermission.PERMISSION_READ_ENCRYPTED -> gattPermission or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
			BLEPermission.PERMISSION_READ_ENCRYPTED_MITM -> gattPermission or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
			BLEPermission.PERMISSION_WRITE -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE
			BLEPermission.PERMISSION_WRITE_ENCRYPTED -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
			BLEPermission.PERMISSION_WRITE_ENCRYPTED_MITM -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
			BLEPermission.PERMISSION_WRITE_SIGNED -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED
			BLEPermission.PERMISSION_WRITE_SIGNED_MITM -> gattPermission or BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM
			BLEPermission.PERMISSION_UNKNOWN -> gattPermission
		}
	}

	return BluetoothGattDescriptor(uuid, gattPermission)
}

