package com.sam.bluepad.data.mappers

import com.sam.bluepad.data.database.entities.DeviceInfoEntity
import com.sam.bluepad.domain.models.ExternalDeviceModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun DeviceInfoEntity.toDevice(
	timezone: TimeZone = TimeZone.currentSystemDefault()
) = ExternalDeviceModel(
	id = id,
	displayName = displayName,
	pairedAt = pairedAt.toLocalDateTime(timezone),
	deviceOs = deviceOs,
	lastSeenAt = lastSeenAt.toLocalDateTime(timezone)
)

fun ExternalDeviceModel.toEntity(
	timezone: TimeZone = TimeZone.currentSystemDefault()
) = DeviceInfoEntity(
	id = id,
	displayName = displayName,
	deviceOs = deviceOs,
	pairedAt = pairedAt?.toInstant(timezone) ?: Clock.System.now(),
	lastSeenAt = lastSeenAt?.toInstant(timezone) ?: Clock.System.now(),
)