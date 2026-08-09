package com.sam.bluepad.common.utils

import okio.Path

interface IFilesProvider {


    fun filesDirectory(): Path

    fun cacheDirectory(): Path
}
