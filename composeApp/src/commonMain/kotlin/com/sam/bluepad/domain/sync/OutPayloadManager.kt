package com.sam.bluepad.domain.sync

import com.sam.bluepad.domain.sync.models.FragmentedDataBlock
import com.sam.bluepad.domain.sync.models.SyncDataPayload

/**
 * Payload data dis-mantle
 */
interface OutPayloadManager {

    /**
     * Prepares and fragments the data based on [type] and store it
     * @property type SyncPayload type to determined what to fragment
     * @return [Result] success or failure as per the case
     * @see SyncDataPayload
     */
    suspend fun prepareChunks(type: SyncDataPayload.Outgoing): Result<Unit>

    /**
     * Retrieves the next available chunk in the sequence.
     * @return Result as [FragmentedDataBlock] or failure if no more chunks exist.
     */
    suspend fun getNextChunk(): Result<FragmentedDataBlock>

    /**
     * Upon sending a chunk an ack will be received call this to mark the ack
     */
    suspend fun markChunkAck(seq: Int)

    /**
     * Checks if there are more chunks left to send.
     */
    suspend fun getHasMoreChunks(): Boolean

    /**
     * Clears the current dismantled state to prepare for a new object.
     */
    suspend fun reset()
}