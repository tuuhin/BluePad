package com.sam.bluepad.data.sync_diff.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("dc")
sealed interface SyncDiffChangesDTO {

    @Serializable
    @SerialName("_in")
    data class InsertChange(
        @SerialName("n") val incoming: SyncSnapshotDto
    ) : SyncDiffChangesDTO

    @Serializable
    @SerialName("_de")
    data class DeleteChange(
        @SerialName("l") val local: SyncSnapshotDto,
        @SerialName("n") val incoming: SyncSnapshotDto
    ) : SyncDiffChangesDTO

    @Serializable
    @SerialName("_up")
    data class UpdateChange(
        @SerialName("l") val local: SyncSnapshotDto,
        @SerialName("n") val incoming: SyncSnapshotDto
    ) : SyncDiffChangesDTO

    @Serializable
    @SerialName("_cf")
    data class ConflictChange(
        @SerialName("l") val local: SyncSnapshotDto,
        @SerialName("n") val incoming: SyncSnapshotDto
    ) : SyncDiffChangesDTO

}
