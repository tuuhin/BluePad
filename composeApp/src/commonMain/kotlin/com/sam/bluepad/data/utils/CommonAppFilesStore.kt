package com.sam.bluepad.data.utils

import okio.Path

expect class CommonAppFilesStore {

    fun filesDirectory(): Path

    fun cacheDirectory(): Path
}
