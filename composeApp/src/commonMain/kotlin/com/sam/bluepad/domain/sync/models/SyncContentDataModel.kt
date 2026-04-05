package com.sam.bluepad.domain.sync.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class SyncContentDataModel(
    val itemId: Uuid,
    val content: String,
    val title: String,
    val isDeleted: Boolean,
    val contentHash: String,
    val version: Int,
    val modifiedByDevice: Uuid? = null,
    val createdByDevice: Uuid? = null,
    val modifiedAt: LocalDateTime,
)
