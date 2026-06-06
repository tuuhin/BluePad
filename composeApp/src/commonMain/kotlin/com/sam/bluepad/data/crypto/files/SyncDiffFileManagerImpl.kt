package com.sam.bluepad.data.crypto.files

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.crypto.SyncDiffFileManager
import com.sam.bluepad.domain.crypto.files.CryptoFilePathProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okio.FileSystem
import kotlin.uuid.Uuid

private const val TAG = "SYNC_DIFF_FILE_MANAGER"

class SyncDiffFileManagerImpl(
    private val fileProvider: CryptoFilePathProvider,
    private val dispatchers: PlatformDispatcherProvider,
) : SyncDiffFileManager {

    private val fs by lazy { FileSystem.SYSTEM }

    private val diffFileFolder by lazy { fileProvider.readCryptoDir() / SYNC_SESSION_DIRECTORY }

    override suspend fun saveContent(sessionId: Uuid, data: ByteArray) {
        createDirectoryIfMissing()

        val filePath = diffFileFolder / "$sessionId.sync"

        return withContext(dispatchers.io) {
            try {
                Logger.d(tag = TAG) { "SAVING SYNC DIFF FILE" }
                fs.write(filePath) {
                    write(data)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.w(tag = TAG, throwable = e) { "FAILED TO SAVED THE SYNC FILE" }
            }
        }
    }

    override suspend fun readContent(sessionId: Uuid): ByteArray {
        createDirectoryIfMissing()

        val filePath = diffFileFolder / "$sessionId.sync"

        return withContext(dispatchers.io) {
            try {
                Logger.d(tag = TAG) { "SAVING SYNC DIFF FILE" }
                fs.read(filePath) {
                    readByteArray()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.w(tag = TAG, throwable = e) { "FAILED TO SAVED THE SYNC FILE" }
                byteArrayOf()
            }
        }
    }

    override suspend fun deleteContent(sessionId: Uuid) {
        val filePath = diffFileFolder / "$sessionId.sync"

        if (!fs.exists(filePath)) return

        return withContext(dispatchers.io) {
            try {
                fs.delete(filePath)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.w(tag = TAG, throwable = e) { "FAILED TO DELETE THE DIFF FILE" }
                byteArrayOf()
            }
        }
    }

    private suspend fun createDirectoryIfMissing() {
        withContext(dispatchers.io) {
            if (fs.exists(diffFileFolder)) return@withContext
            Logger.d(tag = TAG) { "MISSING DIRECTORIES CREATING" }
            FileSystem.SYSTEM.createDirectories(diffFileFolder)
        }
    }

    companion object {
        private const val SYNC_SESSION_DIRECTORY = "sessions"

    }

}
