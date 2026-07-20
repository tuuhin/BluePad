package com.sam.bluepad.data.utils

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual class CommonAppFilesStore {

    private val fs = FileSystem.SYSTEM
    private val tempFs = FileSystem.SYSTEM_TEMPORARY_DIRECTORY

    actual fun filesDirectory(): Path {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = userHome.toPath() / APP_FILES_DIR_NAME
        if (!fs.exists(dir)) fs.createDirectories(dir)
        return dir
    }

    actual fun cacheDirectory(): Path {
        val dir = tempFs / APP_FILES_DIR_NAME
        if (!fs.exists(dir)) fs.createDirectories(dir)
        return dir
    }

    companion object {
        private const val APP_FILES_DIR_NAME = ".bluepad"
    }
}
