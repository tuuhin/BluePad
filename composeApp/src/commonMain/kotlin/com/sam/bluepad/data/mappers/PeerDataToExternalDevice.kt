package com.sam.bluepad.data.mappers

import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel

fun BLEPeerData.toExternalDevice(): ExternalDeviceModel {
	return ExternalDeviceModel(
		id = deviceId,
		displayName = deviceName,
		deviceOs = deviceOs ?: DevicePlatformOS.UNKNOWN,
	)
}