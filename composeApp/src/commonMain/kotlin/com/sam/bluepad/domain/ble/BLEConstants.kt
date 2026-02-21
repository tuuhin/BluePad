package com.sam.bluepad.domain.ble

import kotlin.uuid.Uuid

object BLEConstants {

    // device discovery
    val DEVICE_INFO_SERVICE_ID = Uuid.parse("d4764ea7-c3d1-426f-bf46-c96f4ee95aa8")
    val DEVICE_INFO_CHARACTERISTICS_ID = Uuid.parse("049d551a-572a-49a4-b72b-feed8028336c")

    // device sync and proximity check
    val SYNC_SERVICE_ID = Uuid.parse("a09b1351-a163-479e-ad1c-c860b6ad0f53")
    val PROXIMITY_SYNC_CHARACTERISTICS_ID = Uuid.parse("049d551a-572a-49a4-b72b-feed8028336c")
    val SYNC_DATA_CHARACTERISTICS_ID = Uuid.parse("9843c95e-db7f-45b8-b876-189ff6142126")

    // connection mtu
    const val REQUESTED_MTU = 512 + 3

    // client characteristics config
    val CCC_DESCRIPTOR = Uuid.parse("00002902-0000-1000-8000-00805F9b34FB")

    // client characteristics config value
    val BLE_DESCRIPTOR_ENABLE_NOTIFICATION = byteArrayOf(0x02, 0x00)
    val BLE_DESCRIPTOR_ENABLE_INDICATION = byteArrayOf(0x01, 0x00)
    val BLE_DESCRIPTOR_DISABLE_NOTIFICATION = byteArrayOf(0x00, 0x00)
}