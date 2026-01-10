package com.sam.bluepad.domain.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

data class CreateSketchModel(
	val title: String,
	val content: String,
	val id: Uuid = Uuid.random(),
	val createdAt: LocalDateTime = Clock.System.now()
		.toLocalDateTime(TimeZone.currentSystemDefault()),
	val modifiedAt: LocalDateTime = createdAt,
)
