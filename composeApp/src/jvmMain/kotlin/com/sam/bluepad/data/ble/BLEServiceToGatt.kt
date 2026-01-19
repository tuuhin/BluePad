package com.sam.bluepad.data.ble

import com.sam.ble_common.Characteristic
import com.sam.ble_common.Service
import com.sam.bluepad.domain.ble.BLEConstants

object BLEServiceToGatt {

	val deviceDiscoveryService: Service =
		Service.builder(BLEConstants.transportServiceId.toHexDashString())
			.addCharacteristics(
				listOf(
					Characteristic.builder(BLEConstants.deviceIdCharacteristics.toHexDashString())
						.canRead(true)
						.build(),
					Characteristic.builder(BLEConstants.deviceNameCharacteristic.toHexDashString())
						.canRead(true)
						.build(),
					Characteristic.builder(BLEConstants.connectionNonceCharacteristic.toHexDashString())
						.canRead(true)
						.build(),
					Characteristic.builder(BLEConstants.deviceOSCharacteristics.toHexDashString())
						.canRead(true)
						.build()
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