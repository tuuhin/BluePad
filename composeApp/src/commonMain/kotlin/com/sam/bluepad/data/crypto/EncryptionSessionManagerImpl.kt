package com.sam.bluepad.data.crypto

import com.sam.bluepad.domain.crypto.EncryptionManager
import com.sam.bluepad.domain.crypto.EncryptionSessionManager
import com.sam.bluepad.domain.crypto.KeyEncryptionManager
import com.sam.bluepad.domain.crypto.KeyFileManager
import com.sam.bluepad.domain.crypto.SyncDiffFileManager
import com.sam.bluepad.domain.crypto.exception.MissingKeyFileException
import com.sam.bluepad.domain.crypto.models.KeyEncryptionResult
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlin.uuid.Uuid

class EncryptionSessionManagerImpl private constructor(
    private val fileManager: KeyFileManager,
    private val keyEncryptionManager: KeyEncryptionManager,
    private val encryptionManager: EncryptionManager,
    private val syncFileManager: SyncDiffFileManager,
    private val cryptoProvider: CryptographyProvider,
) : EncryptionSessionManager {

    constructor(
        fileManager: KeyFileManager,
        keyEncryptionManager: KeyEncryptionManager,
        encryptionManager: EncryptionManager,
        syncFileManager: SyncDiffFileManager
    ) : this(
        fileManager = fileManager,
        keyEncryptionManager = keyEncryptionManager,
        encryptionManager = encryptionManager,
        syncFileManager = syncFileManager,
        cryptoProvider = CryptographyProvider.Default,
    )

    private val algo by lazy { cryptoProvider.get(AES.CBC) }

    private suspend fun createAndSaveKey(): KeyEncryptionResult {
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
        } catch (_: MissingKeyFileException) {
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
            throw MissingKeyFileException()
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
