package com.sam.bluepad.data.sync.dto

import com.sam.bluepad.data.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
@SerialName("_sdf")
sealed class SyncDataFrame(@ProtoNumber(10) val type: BLESyncDataType) {

    @Serializable
    @SerialName("md")
    data class Metadata(
        @ProtoNumber(1) val contentHash: String,
        @ProtoNumber(2) val title: String,
        @ProtoNumber(3) val itemId: Uuid,
        @ProtoNumber(4) val version: Int,

        @Serializable(InstantSerializer::class)
        @ProtoNumber(5) val lastModified: Instant,
    ) : SyncDataFrame(type = BLESyncDataType.METADATA)

    @Serializable
    @SerialName("cr")
    data class ContentRequest(
        @ProtoNumber(1) val contentId: Uuid
    ) : SyncDataFrame(BLESyncDataType.CONTENT_REQUEST)

    @Serializable
    @SerialName("ct")
    data class Content(
        @ProtoNumber(1) val itemId: Uuid,
        @ProtoNumber(2) val content: String,
        @ProtoNumber(3) val title: String,
        @ProtoNumber(4) val isDeleted: Boolean,
        @ProtoNumber(5) val contentHash: String,
        @ProtoNumber(6) val version: Int,
        @ProtoNumber(7) val modifiedByDevice: Uuid? = null,
        @ProtoNumber(8) val createdByDevice: Uuid? = null,

        @Serializable(InstantSerializer::class)
        @ProtoNumber(9) val modifiedAt: Instant,
    ) : SyncDataFrame(BLESyncDataType.CONTENT)

}

