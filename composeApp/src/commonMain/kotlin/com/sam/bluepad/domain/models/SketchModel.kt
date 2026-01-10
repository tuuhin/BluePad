package com.sam.bluepad.domain.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class SketchModel(
	val id: Uuid,
	val createdAt: LocalDateTime,
	val modifiedAt: LocalDateTime,
	val title: String,
	val content: String,
	val contentHash: String = "",
	val version: Int = 0,
	val isDeleted: Boolean = false,
	val modifiedByDeviceId: Uuid? = null,
	val createdByDeviceId: Uuid? = null,
)