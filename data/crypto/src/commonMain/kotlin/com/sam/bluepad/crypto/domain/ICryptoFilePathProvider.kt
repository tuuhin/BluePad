package com.sam.bluepad.crypto.domain

import okio.Path

internal fun interface ICryptoFilePathProvider {


    fun readCryptoDir(): Path
}
