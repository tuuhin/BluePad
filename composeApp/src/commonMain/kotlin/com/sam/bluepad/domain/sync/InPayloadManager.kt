package com.sam.bluepad.domain.sync

import com.sam.bluepad.domain.sync.models.SyncDataPayload
import kotlin.uuid.Uuid

/**
 * Sync data re-assembler
 */
interface InPayloadManager {

    /**
     * Buffers an incoming chunk.
     * @param seq The order index of the chunk.
     * @param payload The raw data string.
     */
    suspend fun addIncomingPayloadChunk(seq: Int, payload: String)

    /**
     * Processes buffered chunks to extract Metadata IDs.
     * Clears the buffer automatically upon successful processing.
     * @return [Uuid]s for which the metadata aren't thing
     */
    suspend fun processData(): Result<SyncDataPayload.ProcessedResult>

    /**
     * Forcefully clears all buffered chunks.
     */
    fun clearBuffer()
}