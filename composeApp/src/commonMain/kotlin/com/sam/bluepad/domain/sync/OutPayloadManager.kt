package com.sam.bluepad.domain.sync

import com.sam.bluepad.domain.sync.models.FragmentedDataBlock
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import kotlin.uuid.Uuid

/**
 * Payload data dis-mantle
 */
interface OutPayloadManager {

    /**
     * Prepares and fragments the payload data associated with the given [sessionId] based on its [type],
     * then stores the resulting chunks for transmission.
     *
     * @param sessionId The unique identifier for the current sync session.
     * @param type The specific [SyncDataPayload.Outgoing] payload type that determines how data is fragmented.
     * @return [Result.success] with [Unit] if fragmentation succeeds, or [Result.failure] if payload preparation fails.
     * @see SyncDataPayload
     */
    suspend fun prepareChunks(sessionId: Uuid, type: SyncDataPayload.Outgoing): Result<Unit>

    /**
     * Retrieves the next available data block in the payload sequence for a specific session.
     *
     * @param sessionId The unique identifier for the current sync session.
     * @return [Result.success] containing the next [FragmentedDataBlock], or [Result.failure] if an error occurs or no further chunks exist.
     */
    suspend fun getNextChunk(sessionId: Uuid): Result<FragmentedDataBlock>

    /**
     * Marks a specific chunk sequence number as acknowledged by the receiver.
     *
     * @param sessionId The unique identifier for the current sync session.
     * @param seq The sequence number of the acknowledged chunk.
     */
    suspend fun markChunkAck(sessionId: Uuid, seq: UInt)

    /**
     * Checks whether remaining unsent chunks exist for the specified session.
     *
     * @param sessionId The unique identifier for the current sync session.
     * @return `true` if more chunks are pending transmission; `false` otherwise.
     */
    suspend fun getHasMoreChunks(sessionId: Uuid): Boolean

    /**
     * Clears all temporary dismantled payload states and resets the manager for a new operation.
     */
    suspend fun reset()
}
