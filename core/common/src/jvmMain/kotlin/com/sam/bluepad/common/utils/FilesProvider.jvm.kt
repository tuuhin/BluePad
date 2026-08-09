package com.sam.bluepad.common.utils

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Factory

@Factory(binds = [IFilesProvider::class])
actual class FilesProvider : IFilesProvider {


    private val fs = FileSystem.SYSTEM
    private val tempFs = FileSystem.SYSTEM_TEMPORARY_DIRECTORY

    actual override fun filesDirectory(): Path {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = userHome.toPath() / APP_FILES_DIR_NAME
        if (!fs.exists(dir)) fs.createDirectories(dir)
        return dir
    }

    actual override fun cacheDirectory(): Path {
        val dir = tempFs / APP_FILES_DIR_NAME
        if (!fs.exists(dir)) fs.createDirectories(dir)
        return dir
    }

    companion object {


        private const val APP_FILES_DIR_NAME = ".bluepad"
    }
}
