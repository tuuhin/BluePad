package com.sam.bluepad.data.crypto.files

import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.crypto.KeyFileManager
import com.sam.bluepad.domain.crypto.exception.MissingKeyFileException
import com.sam.bluepad.domain.crypto.files.CryptoFilePathProvider
import com.sam.bluepad.domain.crypto.models.KeyEncryptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem

private const val TAG = "SESSION_KEY_PROVIDER"

class KeyFileManagerImpl(
    private val fileProvider: CryptoFilePathProvider,
) : KeyFileManager {

    private val fs by lazy { FileSystem.SYSTEM }
    private val directoryPath by lazy { fileProvider.readCryptoDir() }

    override suspend fun readKeyResult(): KeyEncryptionResult {

        createDirectoryIfMissing()

        val filePath = directoryPath / SESSION_KEY_FILE_NAME
        Logger.d(tag = TAG) { "READING ENCRYPTION KEY AT: $filePath" }

        // check if key present
        if (!fs.exists(filePath)) throw MissingKeyFileException()

        return withContext(Dispatchers.IO) {
            fs.read(filePath) {
                val ivSize = readInt()
                val ivBytes = ByteArray(ivSize)
                readFully(ivBytes)

                val encryptedBytesSize = readInt()
                val encryptedData = ByteArray(encryptedBytesSize)
                readFully(encryptedData)
                KeyEncryptionResult(
                    iv = ivBytes,
                    encrypted = encryptedData,
                    ivSize = ivSize,
                    encryptedSize = encryptedBytesSize,
                )
            }
        }
    }

    override suspend fun saveKeyResult(keyResult: KeyEncryptionResult) {

        createDirectoryIfMissing()
        val filePath = directoryPath / SESSION_KEY_FILE_NAME

        Logger.d(tag = TAG) { "SAVING ENCRYPTION KEY" }

        withContext(Dispatchers.IO) {

            fs.write(filePath) {
                // enter the iv size
                writeInt(keyResult.ivSize)
                write(keyResult.iv)

                // enter the encrypted key
                writeInt(keyResult.encryptedSize)
                write(keyResult.encrypted)

                Logger.d(TAG) { "IV SIZE:${keyResult.ivSize} ENCRYPTED SIZE:${keyResult.encryptedSize}" }
            }
        }
    }

    override suspend fun deleteSavedKey() {
        val filePath = directoryPath / SESSION_KEY_FILE_NAME

        withContext(Dispatchers.IO) {

            if (!fs.exists(filePath)) {
                Logger.d(tag = TAG) { "FILE MISSING NO KEY FILE FOUND" }
                return@withContext
            }

            Logger.d(tag = TAG) { "DELETING KEY FILE" }
            fs.delete(filePath)
        }
    }

    private suspend fun createDirectoryIfMissing() {
        withContext(Dispatchers.IO) {
            if (fs.exists(directoryPath)) return@withContext
            Logger.d(tag = TAG) { "MISSING DIRECTORIES CREATING" }
            FileSystem.SYSTEM.createDirectories(directoryPath)
        }
    }

    companion object {
        const val SESSION_KEY_FILE_NAME = "session_key.enc"
    }
}
