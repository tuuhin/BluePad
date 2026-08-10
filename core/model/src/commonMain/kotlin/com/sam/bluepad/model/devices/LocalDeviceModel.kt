package com.sam.bluepad.model.devices

import com.sam.bluepad.common.models.PlatformOS
import kotlin.uuid.Uuid

data class LocalDeviceModel(
    val uuid: Uuid,
    val aliasName: String = "",
    val platformOS: PlatformOS = PlatformOS.UNKNOWN
)
