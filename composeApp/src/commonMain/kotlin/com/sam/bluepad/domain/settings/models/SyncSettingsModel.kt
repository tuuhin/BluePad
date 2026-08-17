package com.sam.bluepad.domain.settings.models

import com.sam.bluepad.domain.compression.CompressionLevel

data class SyncSettingsModel(
    val syncCompressionLevel: CompressionLevel = CompressionLevel.LEVEL_3,
    val syncPayloadSize: Int = 32,
) {

    companion object {
        const val MIN_SYNC_CHUNK_SIZE = 16

        // number should be lesser than mtu
        const val MAX_SYNC_CHUNK_SIZE = 320
    }
}
