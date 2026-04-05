package com.sam.bluepad.data.ble.utils

import com.sam.ble_common.Characteristic
import com.sam.ble_common.Service
import com.sam.bluepad.domain.ble.BLEConstants

object BLEServiceToGatt {

    val deviceDiscoveryService: Service =
        Service.builder(BLEConstants.DEVICE_INFO_SERVICE_ID.toHexDashString())
            .addCharacteristic(
                Characteristic.builder(BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID.toHexDashString())
                    .canWriteCommand(true)
                    .canRead(true)
                    .build(),
            ).build()


    val deviceSyncService: Service =
        Service.builder(BLEConstants.SYNC_SERVICE_ID.toHexDashString())
            .addCharacteristic(
                Characteristic.builder(BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID.toHexDashString())
                    .canRead(true)
                    .canWriteCommand(true)
                    .canNotify(true)
                    .build(),
            )
            .addCharacteristic(
                Characteristic.builder(BLEConstants.SYNC_DATA_CHARACTERISTICS_ID.toHexDashString())
                    .canWriteCommand(true)
                    .canNotify(true)
                    .build()
            )
            .build()
}