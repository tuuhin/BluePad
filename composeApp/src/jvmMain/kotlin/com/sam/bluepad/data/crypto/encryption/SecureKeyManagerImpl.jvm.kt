package com.sam.bluepad.data.crypto.encryption

import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.crypto.KeyEncryptionManager
import com.sam.bluepad.domain.crypto.models.KeyEncryptionResult
import com.sam.bluepad.platform.native.PlatformEncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "JVM_KEY_ENCRYPTION_MANAGER"

actual class KeyEncryptionManagerImpl : KeyEncryptionManager {

    private val encryptionManager by lazy { PlatformEncryptionManager() }

    override suspend fun encryptKey(key: ByteArray): KeyEncryptionResult {
        return withContext(Dispatchers.Default) {
            val cipherText = encryptionManager.encryptData(key)
            KeyEncryptionResult(cipherText, encryptedSize = cipherText.size)
        }
    }

    override suspend fun decrypt(key: ByteArray, iv: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) {
            encryptionManager.decryptData(key)
        }
    }

    override fun clearKey() {
        Logger.d(tag = TAG) { "KEY IS HANDLED BY THE OS ITSELF NO NEED TO HANDLE IT" }
    }

    override fun close() {
        Logger.d(tag = TAG) { "CLOSING ENCRYPTION MANAGER"}
        encryptionManager.close()
    }
}
