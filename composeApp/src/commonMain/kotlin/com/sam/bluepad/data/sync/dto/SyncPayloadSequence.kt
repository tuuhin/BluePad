package com.sam.bluepad.data.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@SerialName("_sps")
sealed class SyncPayloadSequence {

    @Serializable
    @SerialName("li_md")
    data class MetaData(
        @ProtoNumber(1) val data: List<SyncDataFrame.Metadata>
    ) : SyncPayloadSequence()

    @Serializable
    @SerialName("ct_q")
    data class ContentRequests(
        @ProtoNumber(1) val data: List<SyncDataFrame.ContentRequest>
    ) : SyncPayloadSequence()

    @Serializable
    @SerialName("li_c")
    data class Content(
        @ProtoNumber(1) val data: List<SyncDataFrame.Content>
    ) : SyncPayloadSequence()
}