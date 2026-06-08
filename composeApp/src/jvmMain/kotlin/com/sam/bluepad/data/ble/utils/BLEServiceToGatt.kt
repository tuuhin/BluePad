package com.sam.bluepad.data.ble.utils

import com.sam.ble_advertise.models.Characteristic
import com.sam.ble_advertise.models.Service
import com.sam.bluepad.domain.ble.BLEConstants

object BLEServiceToGatt {

    val deviceDiscoveryService: Service =
        Service.builder(BLEConstants.DEVICE_INFO_SERVICE_ID)
            .addCharacteristic(
                Characteristic.builder(BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID)
                    .canWriteRequest(true)
                    .canRead(true)
                    .build(),
            ).build()


    val deviceSyncService: Service =
        Service.builder(BLEConstants.SYNC_SERVICE_ID)
            .addCharacteristic(
                Characteristic.builder(BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID)
                    .canRead(true)
                    .canWriteCommand(true)
                    // ccc descriptor will be automatically added
                    .canNotify(true)
                    .build(),
            )
            .addCharacteristic(
                Characteristic.builder(BLEConstants.SYNC_DATA_CHARACTERISTICS_ID)
                    .canWriteCommand(true)
                    // ccc descriptor will be automatically added
                    .canIndicate(true)
                    .build(),
            )
            .build()
}
