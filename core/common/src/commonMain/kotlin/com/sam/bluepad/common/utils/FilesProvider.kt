package com.sam.bluepad.common.utils

import okio.Path

expect class FilesProvider : IFilesProvider {


    override fun cacheDirectory(): Path
    override fun filesDirectory(): Path
}
