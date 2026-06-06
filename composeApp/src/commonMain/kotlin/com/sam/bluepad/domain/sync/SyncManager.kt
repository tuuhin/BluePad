package com.sam.bluepad.domain.sync

import com.sam.bluepad.domain.sync.models.SyncContentDataModel
import com.sam.bluepad.domain.sync.models.SyncMetadataModel
import kotlin.uuid.Uuid


interface SyncManager {

    /**
     * Compares incoming metadata from a remote source and compare  against local state to identify
     * which items different
     * @param metadata A list of metadata from the remote source.
     * @return A [Result] containing a list of [Uuid]s for items that require content exchange.
     */
    suspend fun readChangedItemsIds(metadata: List<SyncMetadataModel>): Result<List<Uuid>>

    /**
     * Persists new content data received from a remote into the local source.
     * @param results The content payloads to be saved.
     * @param sessionId Sync session id
     * @return A [Result] indicating success or containing an error if the save operation fails.
     */
    suspend fun performSyncResultsOperation(sessionId: Uuid, results: List<SyncContentDataModel>): Result<Unit>
}
