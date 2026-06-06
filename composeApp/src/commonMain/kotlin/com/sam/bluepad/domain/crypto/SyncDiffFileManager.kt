package com.sam.bluepad.domain.crypto

import kotlin.uuid.Uuid

interface SyncDiffFileManager {
    suspend fun saveContent(sessionId: Uuid, data: ByteArray)
    suspend fun readContent(sessionId: Uuid): ByteArray

    suspend fun deleteContent(sessionId: Uuid)
}
