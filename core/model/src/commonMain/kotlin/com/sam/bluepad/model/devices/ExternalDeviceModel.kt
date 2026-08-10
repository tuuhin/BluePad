package com.sam.bluepad.model.devices

import com.sam.bluepad.common.models.PlatformOS
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class ExternalDeviceModel(
    val id: Uuid,
    val displayName: String? = null,
    val pairedAt: LocalDateTime? = null,
    val lastSeenAt: LocalDateTime? = null,
    val deviceOs: PlatformOS = PlatformOS.UNKNOWN
)
