package com.sam.bluepad.domain.crypto

import com.sam.bluepad.domain.crypto.models.KeyEncryptionResult

interface KeyEncryptionManager {

    suspend fun encryptKey(key: ByteArray): KeyEncryptionResult

    suspend fun decrypt(key: ByteArray, iv: ByteArray): ByteArray

    fun clearKey()
}
