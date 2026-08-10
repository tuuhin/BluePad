package com.sam.bluepad.crypto.data.secrets

import com.sam.bluepad.crypto.domain.IPlatformKeyEncryptionManager
import com.sam.bluepad.model.crypto.EncryptionResult
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformKeyEncryptionManager::class])
internal expect class KeyEncryptionManager : IPlatformKeyEncryptionManager {


    override suspend fun encryptKey(key: ByteArray): EncryptionResult
    override suspend fun decrypt(key: ByteArray, iv: ByteArray): ByteArray
    override fun clearKey()
}
