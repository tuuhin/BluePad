package com.sam.bluepad.domain.crypto.files

import okio.Path

fun interface CryptoFilePathProvider {

    fun readCryptoDir(): Path
}
