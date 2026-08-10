package com.sam.bluepad.crypto.domain

import com.sam.bluepad.model.crypto.EncryptionResult

internal interface IPlatformKeyEncryptionManager {


    suspend fun encryptKey(key: ByteArray): EncryptionResult

    suspend fun decrypt(key: ByteArray, iv: ByteArray): ByteArray

    fun clearKey()
}
