package com.sam.bluepad.domain.sync.models

import kotlin.uuid.Uuid

data class SyncContentDataModel(
    val itemId: Uuid,
    val content: String,
)
