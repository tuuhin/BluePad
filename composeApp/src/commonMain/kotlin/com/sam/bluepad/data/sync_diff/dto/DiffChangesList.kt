package com.sam.bluepad.data.sync_diff.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiffChangesList(
    @SerialName("_ss") val diffs: Set<SyncDiffChangesDTO> = emptySet()
)
