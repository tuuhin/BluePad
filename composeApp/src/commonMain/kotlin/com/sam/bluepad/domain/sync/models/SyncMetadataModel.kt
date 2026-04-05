package com.sam.bluepad.domain.sync.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class SyncMetadataModel(
    val contentHash: String,
    val title: String,
    val itemId: Uuid,
    val version: Int,
    val lastModified: LocalDateTime,
)
