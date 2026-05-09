package com.sam.bluepad.domain.crypto

import kotlin.uuid.Uuid

interface EncryptionSessionManager {
	suspend fun encryptDataAndSave(sessionId: Uuid, data: ByteArray)

	suspend fun decryptAndReadData(sessionId: Uuid): ByteArray

	suspend fun deleteSessionData(sessionId: Uuid)
}
