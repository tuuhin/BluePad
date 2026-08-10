package com.sam.bluepad.crypto.data.files

import co.touchlab.kermit.Logger
import com.sam.bluepad.common.utils.ICoroutineDispatchersProvider
import com.sam.bluepad.crypto.domain.ICryptoFilePathProvider
import com.sam.bluepad.crypto.domain.ICryptoKeysFileProvider
import com.sam.bluepad.crypto.domain.exceptions.CryptoMissingKeyException
import com.sam.bluepad.model.crypto.EncryptionResult
import kotlinx.coroutines.withContext
import okio.FileSystem
import org.koin.core.annotation.Factory

private const val TAG = "SESSION_KEY_PROVIDER"

@Factory(binds = [ICryptoKeysFileProvider::class])
internal class CryptoKeysFileProvider(
    private val fileProvider: ICryptoFilePathProvider,
    private val dispatchers: ICoroutineDispatchersProvider,
) : ICryptoKeysFileProvider {


    private val fs by lazy { FileSystem.SYSTEM }
    private val directoryPath get() = fileProvider.readCryptoDir()

    override suspend fun readKeyResult(): EncryptionResult {

        createDirectoryIfMissing()

        val filePath = directoryPath / SESSION_KEY_FILE_NAME
        Logger.d(tag = TAG) { "READING ENCRYPTION KEY AT: $filePath" }

        // check if key present
        if (!fs.exists(filePath)) throw CryptoMissingKeyException()

        return withContext(dispatchers.io) {
            fs.read(filePath) {
                val ivSize = readInt()
                val ivBytes = ByteArray(ivSize)
                readFully(ivBytes)

                val encryptedBytesSize = readInt()
                val encryptedData = ByteArray(encryptedBytesSize)
                readFully(encryptedData)
                EncryptionResult(
                    iv = ivBytes,
                    encrypted = encryptedData,
                    ivSize = ivSize,
                    encryptedSize = encryptedBytesSize,
                )
            }
        }
    }

    override suspend fun saveKeyResult(keyResult: EncryptionResult) {

        createDirectoryIfMissing()
        val filePath = directoryPath / SESSION_KEY_FILE_NAME

        Logger.d(tag = TAG) { "SAVING ENCRYPTION KEY" }

        withContext(dispatchers.io) {

            fs.write(filePath) {
                // enter the iv size
                writeInt(keyResult.ivSize)
                write(keyResult.iv)

                // enter the encrypted key
                writeInt(keyResult.encryptedSize)
                write(keyResult.encrypted)

                Logger.d(tag = TAG) { "IV SIZE:${keyResult.ivSize} ENCRYPTED SIZE:${keyResult.encryptedSize}" }
            }
        }
    }

    override suspend fun deleteSavedKey() {
        val filePath = directoryPath / SESSION_KEY_FILE_NAME

        withContext(dispatchers.io) {

            if (!fs.exists(filePath)) {
                Logger.d(tag = TAG) { "FILE MISSING NO KEY FILE FOUND" }
                return@withContext
            }

            Logger.d(tag = TAG) { "DELETING KEY FILE" }
            fs.delete(filePath)
        }
    }

    private suspend fun createDirectoryIfMissing() {
        withContext(dispatchers.io) {
            if (fs.exists(directoryPath)) return@withContext
            Logger.d(tag = TAG) { "MISSING DIRECTORIES CREATING" }
            FileSystem.SYSTEM.createDirectories(directoryPath)
        }
    }

    companion object {


        const val SESSION_KEY_FILE_NAME = "session_key.enc"
    }
}
