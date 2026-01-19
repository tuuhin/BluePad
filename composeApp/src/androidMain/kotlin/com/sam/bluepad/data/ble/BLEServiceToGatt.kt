package com.sam.bluepad.data.ble

import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLEPermission
import com.sam.bluepad.domain.ble.BLEPropertyType
import com.sam.bluepad.domain.ble.BLEServiceType

object BLEServiceToGatt {

	val deviceDiscoveryService = bleServiceOf(
		uuid = BLEConstants.transportServiceId,
		serviceType = BLEServiceType.PRIMARY,
		characteristics = listOf(
			bleCharacteristicsOf(
				BLEConstants.deviceIdCharacteristics,
				listOf(BLEPropertyType.PROPERTY_READ),
				listOf(BLEPermission.PERMISSION_READ)
			),
			bleCharacteristicsOf(
				BLEConstants.deviceNameCharacteristic,
				listOf(BLEPropertyType.PROPERTY_READ),
				listOf(BLEPermission.PERMISSION_READ)
			),
			bleCharacteristicsOf(
				BLEConstants.connectionNonceCharacteristic,
				listOf(BLEPropertyType.PROPERTY_READ),
				listOf(BLEPermission.PERMISSION_READ)
			),
			bleCharacteristicsOf(
				BLEConstants.deviceOSCharacteristics,
				listOf(BLEPropertyType.PROPERTY_READ),
				listOf(BLEPermission.PERMISSION_READ)
			),
		)
	)

	val deviceSyncService = bleServiceOf(
		BLEConstants.syncServiceId,
		serviceType = BLEServiceType.PRIMARY,
		characteristics = listOf(
			bleCharacteristicsOf(
				BLEConstants.allowSyncCharacteristics,
				listOf(BLEPropertyType.PROPERTY_READ),
				listOf(BLEPermission.PERMISSION_READ),
			),
			bleCharacteristicsOf(
				BLEConstants.deviceIdCharacteristics,
				listOf(BLEPropertyType.PROPERTY_READ, BLEPropertyType.PROPERTY_WRITE),
				listOf(BLEPermission.PERMISSION_READ, BLEPermission.PERMISSION_WRITE)
			),
			bleCharacteristicsOf(
				BLEConstants.connectionNonceCharacteristic,
				listOf(BLEPropertyType.PROPERTY_READ, BLEPropertyType.PROPERTY_WRITE),
				listOf(BLEPermission.PERMISSION_READ, BLEPermission.PERMISSION_WRITE)
			),
			bleCharacteristicsOf(
				BLEConstants.receiverDeviceIdCharacteristics,
				listOf(BLEPropertyType.PROPERTY_READ, BLEPropertyType.PROPERTY_WRITE),
				listOf(BLEPermission.PERMISSION_READ, BLEPermission.PERMISSION_WRITE)
			),
		)
	)
}