package com.sam.bluepad.domain.sync_diff

import kotlin.uuid.Uuid

fun interface SyncDataSaver {

    /**
     * Saves the list of [SyncChanges] in the db with modified device marked by [externalDeviceId]
     */
    suspend fun submitSyncChanges(changes: List<SyncChanges>, externalDeviceId: Uuid): Result<Unit>
}
