package com.sam.bluepad.crypto.domain

internal interface IEncryptionManager {


    suspend fun encrypt(key: ByteArray, data: ByteArray): ByteArray
    suspend fun decrypt(key: ByteArray, data: ByteArray): ByteArray

}
