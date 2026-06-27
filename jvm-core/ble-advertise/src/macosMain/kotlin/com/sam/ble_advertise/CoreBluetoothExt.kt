package com.sam.ble_advertise

import com.sam.ble_advertise.models.BLECharacteristicsModel
import platform.CoreBluetooth.CBAttributePermissions
import platform.CoreBluetooth.CBAttributePermissionsReadable
import platform.CoreBluetooth.CBAttributePermissionsWriteable
import platform.CoreBluetooth.CBCharacteristicProperties
import platform.CoreBluetooth.CBCharacteristicPropertyIndicate
import platform.CoreBluetooth.CBCharacteristicPropertyNotify
import platform.CoreBluetooth.CBCharacteristicPropertyRead
import platform.CoreBluetooth.CBCharacteristicPropertyWrite
import platform.CoreBluetooth.CBCharacteristicPropertyWriteWithoutResponse
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerStatePoweredOff
import platform.CoreBluetooth.CBPeripheralManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheralManagerStateResetting
import platform.CoreBluetooth.CBPeripheralManagerStateUnauthorized
import platform.CoreBluetooth.CBPeripheralManagerStateUnknown
import platform.CoreBluetooth.CBPeripheralManagerStateUnsupported
import platform.darwin.NSUInteger

internal val BLECharacteristicsModel.toCBProperties: NSUInteger
    get() {
        var properties: CBCharacteristicProperties = 0u
        if (canRead) properties = properties or CBCharacteristicPropertyRead
        if (canWrite) properties = properties or CBCharacteristicPropertyWrite
        if (canWriteNoResponse) properties = properties or CBCharacteristicPropertyWriteWithoutResponse
        if (canNotify) properties = properties or CBCharacteristicPropertyNotify
        if (canIndicate) properties = properties or CBCharacteristicPropertyIndicate
        return properties
    }

internal val BLECharacteristicsModel.toCBPermissions: NSUInteger
    get() {
        var permissions: CBAttributePermissions = NSUInteger.MIN_VALUE
        if (canRead) permissions = permissions or CBAttributePermissionsReadable
        if (canWrite) permissions = permissions or CBAttributePermissionsWriteable
        return permissions
    }


internal fun CBPeripheralManager.bluetoothStateAsString() = when (state) {
    CBPeripheralManagerStateResetting -> "PERIPHERAL STATE RESETTING"
    CBPeripheralManagerStatePoweredOn -> "PERIPHERAL POWER ON"
    CBPeripheralManagerStateUnsupported -> "PERIPHERAL MODE UNSUPPORTED"
    CBPeripheralManagerStateUnauthorized -> "PERIPHERAL UNAUTHORIZED"
    CBPeripheralManagerStatePoweredOff -> "PERIPHERAL STOPPED"
    CBPeripheralManagerStateUnknown -> "PERIPHERAL INVALID STATE"
    else -> throw IllegalStateException("Invalid peripheral state")
}
