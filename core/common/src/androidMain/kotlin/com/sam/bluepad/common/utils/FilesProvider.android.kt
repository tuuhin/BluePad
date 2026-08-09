package com.sam.bluepad.common.utils

import android.content.Context
import okio.Path
import okio.Path.Companion.toOkioPath
import org.koin.core.annotation.Factory

@Factory(binds = [IFilesProvider::class])
actual class FilesProvider(private val context: Context) : IFilesProvider {


    actual override fun filesDirectory(): Path = context.filesDir.toOkioPath()
    actual override fun cacheDirectory(): Path = context.cacheDir.toOkioPath()
}
