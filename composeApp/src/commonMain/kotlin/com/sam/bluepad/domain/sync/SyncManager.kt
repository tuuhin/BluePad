package com.sam.bluepad.domain.sync

import com.sam.bluepad.domain.sync.models.SyncContentDataModel
import com.sam.bluepad.domain.sync.models.SyncMetadataModel
import kotlin.uuid.Uuid


interface SyncManager {

    /**
     * Compares incoming metadata from a peer against local state to identify 
     * which items are out of sync or missing.
     * 
     * @param incoming A list of metadata from the remote source.
     * @return A [Result] containing a list of [Uuid]s for items that require content exchange.
     */
    suspend fun findChangedItems(incoming: List<SyncMetadataModel>): Result<List<Uuid>>

    /**
     * Fetches and prepares the full content data for a specific set of items 
     * intended for transmission to a peer.
     * 
     * @param itemIds The unique identifiers of the items to prepare.
     * @return A [Result] containing a list of [SyncContentDataModel] ready for exchange.
     */
    suspend fun fetchContentForExchange(itemIds: List<Uuid>): Result<List<SyncContentDataModel>>

    /**
     * Integrates and persists new content data received from a peer into the local repository.
     * 
     * @param data The content payloads to be saved.
     * @return A [Result] indicating success or containing an error if the save operation fails.
     */
    suspend fun saveSyncContent(data: List<SyncContentDataModel>): Result<Unit>

    /**
     * Generates metadata for all locally managed items to be shared with a peer 
     * during the initial synchronization handshake.
     * 
     * @return A [Result] containing the list of local [SyncMetadataModel].
     */
    suspend fun getLocalSyncMetadata(): Result<List<SyncMetadataModel>>
}
