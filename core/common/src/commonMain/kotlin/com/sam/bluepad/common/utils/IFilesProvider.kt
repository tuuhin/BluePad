package com.sam.bluepad.common.utils

import okio.Path
import org.jetbrains.annotations.VisibleForTesting

interface IFilesProvider {


    fun filesDirectory(): Path

    fun cacheDirectory(): Path

    @VisibleForTesting
    fun deletePath(): Boolean
}
