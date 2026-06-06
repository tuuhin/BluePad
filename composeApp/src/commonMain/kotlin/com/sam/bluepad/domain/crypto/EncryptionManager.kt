package com.sam.bluepad.domain.crypto

interface EncryptionManager {

    suspend fun encrypt(key: ByteArray, data: ByteArray): ByteArray
    suspend fun decrypt(key: ByteArray, data: ByteArray): ByteArray
}
