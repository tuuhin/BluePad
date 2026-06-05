package com.sam.bluepad.domain.sync_diff

import kotlin.uuid.Uuid

fun interface SyncDataSessionReader {

    /**
     * Reads the saved session data of [SyncChanges]  via session id
     * Warning: It's a one time call reading will delete the data from the local machine
     */
    suspend fun readSyncSession(session: Uuid): Result<List<SyncChanges>>
}
