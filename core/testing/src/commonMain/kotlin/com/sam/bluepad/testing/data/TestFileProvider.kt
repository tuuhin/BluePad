package com.sam.bluepad.testing.data

import co.touchlab.kermit.Logger
import com.sam.bluepad.common.utils.IFilesProvider
import okio.FileSystem
import okio.Path

private const val TAG = "TestFileProvider"

class TestFileProvider : IFilesProvider {


    private val fs = FileSystem.SYSTEM
    private val tempFs = FileSystem.SYSTEM_TEMPORARY_DIRECTORY

    override fun filesDirectory(): Path {
        val dir = tempFs / APP_FILES_DIR_NAME
        if (!fs.exists(dir)) fs.createDirectories(dir)
        Logger.d(tag = TAG) { "FILES PATH :$dir" }
        return dir
    }

    override fun cacheDirectory(): Path {
        val dir = tempFs / APP_FILES_DIR_NAME
        if (!fs.exists(dir)) fs.createDirectories(dir)
        Logger.d(tag = TAG) { "CACHE PATH :$dir" }
        return dir
    }

    override fun deletePath(): Boolean {
        val dir = tempFs / APP_FILES_DIR_NAME
        if (!fs.exists(dir)) return true
        return try {
            Logger.d(tag = TAG) { "DELETING FILE PATH :$dir" }
            fs.deleteRecursively(dir)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {


        private const val APP_FILES_DIR_NAME = "bluepad_test"
    }
}
