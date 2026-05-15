package com.sam.bluepad.domain.sync_diff

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class SyncSnapshotModel(
    val id: Uuid,
    val version: Int,
    val title: String,
    val content: String,
    val contentHash: String,
    val modifiedAt: LocalDateTime,
    val isDeleted: Boolean,
)
