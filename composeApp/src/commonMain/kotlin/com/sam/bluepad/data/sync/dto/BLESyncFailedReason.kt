package com.sam.bluepad.data.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@SerialName("__sfr")
enum class BLESyncFailedReason {

	@ProtoNumber(1)
	TAMPERED_DATA,

	@ProtoNumber(2)
	CONTENT_SAME,

	@ProtoNumber(3)
	MISSING_FLAG,

	@ProtoNumber(4)
	INVALID_STATE,
}
