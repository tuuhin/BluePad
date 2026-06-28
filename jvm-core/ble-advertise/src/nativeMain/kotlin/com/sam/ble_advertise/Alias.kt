package com.sam.ble_advertise

internal typealias OnServiceAdded = (serviceUuid: String, success: Boolean, errorCode: Int) -> Unit
internal typealias OnServiceChanged = (status: Int) -> Unit
internal typealias OnReadCharacteristics = (deviceAddress: String, serviceUuid: String, characteristicUuid: String, status: Int) -> String
internal typealias OnWriteCharacteristics = (deviceAddress: String, serviceUuid: String, characteristicUuid: String, value: ByteArray, respond: Boolean) -> Int
internal typealias OnReadDescriptor = (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, status: Int) -> String
internal typealias OnWriteDescriptor = (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, value: ByteArray, respond: Boolean) -> Int
internal typealias OnIndicationResult = (deviceAddress: String, characteristicUuid: String, success: Boolean, status: Int, errorCode: Int) -> Unit
