package com.sam.bluepad.domain.ble.models

import com.sam.bluepad.domain.models.DevicePlatformOS
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@Serializable
data class BLEPeerData(
	@ProtoNumber(1) val deviceId: Uuid,
	@ProtoNumber(2) val deviceName: String? = null,
	@ProtoNumber(3) val nonce: String? = null,
	@ProtoNumber(4) val deviceOs: DevicePlatformOS? = null,
)