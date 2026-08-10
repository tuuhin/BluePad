package com.sam.bluepad.crypto.domain

import kotlin.uuid.Uuid

internal interface ICryptoContentManager {


    suspend fun saveContent(sessionId: Uuid, data: ByteArray)
    suspend fun readContent(sessionId: Uuid): ByteArray

    suspend fun deleteContent(sessionId: Uuid)
}
