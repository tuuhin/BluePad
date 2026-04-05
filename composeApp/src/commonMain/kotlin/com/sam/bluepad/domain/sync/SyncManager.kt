package com.sam.bluepad.domain.sync

import com.sam.bluepad.domain.sync.models.SyncContentDataModel
import com.sam.bluepad.domain.sync.models.SyncMetadataModel
import kotlin.uuid.Uuid


interface SyncManager {

    /**
     * Compares incoming metadata from a peer against local state to identify
     * which items are out of sync or missing.
     *
     * @param metadata A list of metadata from the remote source.
     * @return A [Result] containing a list of [Uuid]s for items that require content exchange.
     */
    suspend fun computeUpdatedOrNewItems(metadata: List<SyncMetadataModel>): Result<List<Uuid>>

    /**
     * Integrates and persists new content data received from a peer into the local repository.
     *
     * @param results The content payloads to be saved.
     * @return A [Result] indicating success or containing an error if the save operation fails.
     */
    suspend fun performSyncResultsOperation(results: List<SyncContentDataModel>): Result<Unit>
}
