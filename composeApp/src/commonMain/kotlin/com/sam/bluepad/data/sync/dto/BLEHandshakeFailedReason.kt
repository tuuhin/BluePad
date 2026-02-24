package com.sam.bluepad.data.sync.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
enum class BLEHandshakeFailedReason {
    @ProtoNumber(1)
    UNKNOWN_SENDER,

    @ProtoNumber(2)
    INVALID_RECEIVER,

    @ProtoNumber(3)
    INVALID_INCOMING_DATA,

    @ProtoNumber(4)
    TAMPERED_DATA;
}