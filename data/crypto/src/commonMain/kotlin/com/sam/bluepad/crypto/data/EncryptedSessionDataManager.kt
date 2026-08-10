package com.sam.bluepad.crypto.data

import com.sam.bluepad.crypto.domain.ICryptoContentManager
import com.sam.bluepad.crypto.domain.ICryptoKeysFileProvider
import com.sam.bluepad.crypto.domain.IEncryptionManager
import com.sam.bluepad.crypto.domain.IPlatformKeyEncryptionManager
import com.sam.bluepad.crypto.domain.exceptions.CryptoMissingKeyException
import com.sam.bluepad.domain.crypto.IEncryptionSessionManager
import com.sam.bluepad.model.crypto.EncryptionResult
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory(binds = [IEncryptionSessionManager::class])
internal class EncryptedSessionDataManager(
    private val fileManager: ICryptoKeysFileProvider,
    private val keyEncryptionManager: IPlatformKeyEncryptionManager,
    private val encryptionManager: IEncryptionManager,
    private val syncFileManager: ICryptoContentManager,
    private val cryptoProvider: CryptographyProvider = CryptographyProvider.Default,
) : IEncryptionSessionManager {


    private val algo by lazy { cryptoProvider.get(AES.CBC) }

    private suspend fun createAndSaveKey(): EncryptionResult {
        // create a new key
        val key = algo.keyGenerator().generateKey()
        val keyAsBytes = key.encodeToByteArray(AES.Key.Format.RAW)
        // encrypt the key
        val result = keyEncryptionManager.encryptKey(keyAsBytes)
        // store the key
        fileManager.saveKeyResult(result)
        return result
    }

    override suspend fun encryptDataAndSave(sessionId: Uuid, data: ByteArray) {
        // try to read the encrypted key from the disk otherwise create a new key and save it
        val keyResult = try {
            fileManager.readKeyResult()
        } catch (_: CryptoMissingKeyException) {
            createAndSaveKey()
        }
        // decode the given key
        val decodedKey = keyEncryptionManager.decrypt(keyResult.encrypted, keyResult.iv)
        // convert it into aes key
        val encryptedData = encryptionManager.encrypt(decodedKey, data)
        //save the content
        syncFileManager.saveContent(sessionId, encryptedData)
    }


    override suspend fun decryptAndReadData(sessionId: Uuid): ByteArray {
        // decode the given key
        val keyResult = try {
            fileManager.readKeyResult()
        } catch (_: Exception) {
            throw CryptoMissingKeyException()
        }
        val decodedKey = keyEncryptionManager.decrypt(keyResult.encrypted, keyResult.iv)
        // read the content
        val content = syncFileManager.readContent(sessionId = sessionId)
        // decrypt the content
        val decryptedData = encryptionManager.decrypt(decodedKey, content)
        return decryptedData
    }

    override suspend fun deleteSessionData(sessionId: Uuid) {
        supervisorScope {
            val op1 = async { fileManager.deleteSavedKey() }
            val op2 = async { syncFileManager.deleteContent(sessionId) }
            awaitAll(op1, op2)
        }
    }
}
