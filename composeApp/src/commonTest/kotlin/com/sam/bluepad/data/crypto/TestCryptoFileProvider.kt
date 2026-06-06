package com.sam.bluepad.data.crypto

import com.sam.bluepad.domain.crypto.files.CryptoFilePathProvider
import okio.FileSystem
import okio.Path
import kotlin.time.Clock

class TestCryptoFileProvider : CryptoFilePathProvider {

    private val uniqueId = Clock.System.now().toEpochMilliseconds()

    override fun readCryptoDir(): Path {
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
        // Every test run gets its own folder: test_folder_1715284...
        val tempFolder = tempDir / "test_folder_$uniqueId"

        if (!FileSystem.SYSTEM.exists(tempFolder)) {
            FileSystem.SYSTEM.createDirectories(tempFolder)
        }
        return tempFolder
    }
}
