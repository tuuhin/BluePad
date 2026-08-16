package com.sam.bluepad.domain.sync

import com.sam.bluepad.domain.sync.models.SyncDataPayload
import kotlin.uuid.Uuid

/**
 * Manages the buffering, reassembly, and processing lifecycle of incoming fragmented sync payload data.
 */
interface InPayloadManager {

    /**
     * Buffers an incoming payload chunk for a specific sync session.
     *
     * @param sessionId The unique identifier for the active sync session.
     * @param seq The zero-based sequence or order index of the chunk.
     * @param payload The raw string payload corresponding to the chunk.
     */
    suspend fun addIncomingPayloadChunk(sessionId: Uuid, seq: UInt, payload: String)

    /**
     * Reassembles and decodes all buffered chunks for the specified [sessionId], then executes
     * the corresponding sync processing operation.
     *
     * Automatically clears the session buffer upon completion or failure.
     *
     * @param sessionId The unique identifier for the sync session being processed.
     * @return [Result.success] with [SyncDataPayload.ProcessedResult] containing the outcome of processing,
     * or [Result.failure] if decoding or sync processing fails.
     */
    suspend fun processData(sessionId: Uuid): Result<SyncDataPayload.ProcessedResult>

    /**
     * Forcefully clears all buffered chunks for a specific sync session.
     *
     * @param sessionId The unique identifier for the sync session to clear.
     */
    suspend fun clearBuffer(sessionId: Uuid)

    /**
     * Clears all buffered chunk states across all active sync sessions.
     */
    fun clearAllBuffers()
}
