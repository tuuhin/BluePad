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
        @Serializable(InstantSerializer::class) @ProtoNumber(5) val lastModified: Instant,
    ) : SyncDataFrame(type = BLESyncDataType.METADATA)

    @Serializable
    @SerialName("cq")
    data class ContentIDQuery(
        @ProtoNumber(1) val queryId: Uuid
    ) : SyncDataFrame(BLESyncDataType.REQUESTED_CONTENT_IDS)

    @Serializable
    @SerialName("c")
    data class Content(
        @ProtoNumber(1) val itemId: Uuid,
        @ProtoNumber(2) val content: String,
    ) : SyncDataFrame(BLESyncDataType.CONTENT)

}

