package com.sam.bluepad.data.sync_diff.dto

import com.sam.bluepad.data.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class SyncSnapshotDto(
    @SerialName("i") val id: Uuid,
    @SerialName("v") val version: Int,
    @SerialName("t") val title: String,
    @SerialName("c") val content: String,
    @SerialName("h") val contentHash: String,
    @Serializable(InstantSerializer::class)
    @SerialName("mat") val modifiedAt: Instant,
    @SerialName("d") val isDeleted: Boolean,
)
