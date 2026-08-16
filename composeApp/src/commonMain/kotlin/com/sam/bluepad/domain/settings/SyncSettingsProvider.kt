package com.sam.bluepad.domain.settings

import com.sam.bluepad.domain.compression.CompressionLevel
import com.sam.bluepad.domain.settings.models.SyncSettingsModel
import kotlinx.coroutines.flow.Flow

interface SyncSettingsProvider {

    val settingsFlow: Flow<SyncSettingsModel>

    suspend fun settings(): SyncSettingsModel

    suspend fun updateCompressionLevel(level: CompressionLevel)

    suspend fun updatePayloadSize(size: Int)
}
