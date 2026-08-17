package com.sam.bluepad.data.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@SerialName("__scr")
enum class BLESyncCompletionReason {

    @ProtoNumber(1)
    FULL_DUPLEX_SYNC_COMPLETED,

    @ProtoNumber(2)
    CONTENT_ALREADY_SAME
}
