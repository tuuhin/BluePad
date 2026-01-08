package com.sam.bluepad.domain.models

import kotlin.uuid.Uuid

data class LocalDeviceInfoModel(
	val deviceId: Uuid,
	val name: String = "",
)
