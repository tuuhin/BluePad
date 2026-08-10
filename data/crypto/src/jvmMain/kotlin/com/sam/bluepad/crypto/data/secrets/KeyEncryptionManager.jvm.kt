package com.sam.bluepad.crypto.data.secrets

import com.sam.bluepad.crypto.domain.IPlatformKeyEncryptionManager
import com.sam.bluepad.model.crypto.EncryptionResult
import com.sam.bluepad.platform.native.PlatformEncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformKeyEncryptionManager::class])
internal actual class KeyEncryptionManager : IPlatformKeyEncryptionManager {


    actual override suspend fun encryptKey(key: ByteArray): EncryptionResult {
        return withContext(Dispatchers.Default) {
            PlatformEncryptionManager().use { encryptionManager ->
                val cipherText = encryptionManager.encryptData(key)
                EncryptionResult(cipherText, encryptedSize = cipherText.size)
            }
        }
    }

    actual override suspend fun decrypt(key: ByteArray, iv: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) {
            PlatformEncryptionManager().use { encryptionManager ->
                encryptionManager.decryptData(key)
            }
        }
    }

    actual override fun clearKey() {
    }
}
