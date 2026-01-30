package com.sam.bluepad.data.ble

import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLEPermission
import com.sam.bluepad.domain.ble.BLEPropertyType
import com.sam.bluepad.domain.ble.BLEServiceType

object BLEServiceToGatt {

    val deviceDiscoveryService = bleServiceOf(
        uuid = BLEConstants.DEVICE_INFO_SERVICE_ID,
        serviceType = BLEServiceType.PRIMARY,
        characteristics = listOf(
            bleCharacteristicsOf(
                uuid = BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID,
                properties = listOf(
                    BLEPropertyType.PROPERTY_READ,
                    BLEPropertyType.PROPERTY_WRITE
                ),
                permissions = listOf(
                    BLEPermission.PERMISSION_READ,
                    BLEPermission.PERMISSION_WRITE_ENCRYPTED
                )
            ),
        )
    )

    val deviceSyncService = bleServiceOf(
        uuid = BLEConstants.SYNC_SERVICE_ID,
        serviceType = BLEServiceType.PRIMARY,
        characteristics = listOf(
            bleCharacteristicsOf(
                uuid = BLEConstants.SYNC_CHARACTERISTICS_ID,
                properties = listOf(
                    BLEPropertyType.PROPERTY_READ,
                    BLEPropertyType.PROPERTY_WRITE,
                    BLEPropertyType.PROPERTY_NOTIFY
                ),
                permissions = listOf(
                    BLEPermission.PERMISSION_READ,
                    BLEPermission.PERMISSION_WRITE,

                )
            ).apply {
                val descriptor = bleDescriptorOf(
                    uuid = BLEConstants.CCC_DESCRIPTOR,
                    permissions = listOf(
                        BLEPermission.PERMISSION_READ,
                        BLEPermission.PERMISSION_WRITE
                    )
                )
                addDescriptor(descriptor)
            }
        )
    )
}