package com.sam.bluepad.data.ble

import com.sam.ble_common.Characteristic
import com.sam.ble_common.Service
import com.sam.bluepad.domain.ble.BLEConstants

object BLEServiceToGatt {

	val deviceDiscoveryService: Service =
		Service.builder(BLEConstants.discoveryServiceId.toHexDashString())
			.addCharacteristics(
				listOf(
					Characteristic.builder(BLEConstants.deviceInfoCharacteristics.toHexDashString())
						.canWriteRequest(true)
						.canRead(true)
						.build(),
				)
			).build()


	val deviceSyncService: Service =
		Service.builder(BLEConstants.syncServiceId.toHexDashString())
			.addCharacteristics(
				listOf(
					Characteristic.builder(BLEConstants.allowSyncCharacteristics.toHexDashString())
						.canRead(true)
						.build(),
					Characteristic.builder(BLEConstants.deviceIdCharacteristics.toHexDashString())
						.canRead(true)
						.canWriteCommand(true)
						.build(),
					Characteristic.builder(BLEConstants.connectionNonceCharacteristic.toHexDashString())
						.canRead(true)
						.canWriteCommand(true)
						.build(),
					Characteristic.builder(BLEConstants.receiverDeviceIdCharacteristics.toHexDashString())
						.canRead(true)
						.canWriteCommand(true)
						.build(),
				)
			)
			.build()
}