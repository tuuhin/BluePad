package com.sam.bluepad.data.utils

import android.content.Context
import okio.Path
import okio.Path.Companion.toOkioPath

actual class CommonAppFilesStore(private val context: Context) {

    actual fun filesDirectory(): Path = context.filesDir.toOkioPath()
    actual fun cacheDirectory(): Path = context.cacheDir.toOkioPath()
}
