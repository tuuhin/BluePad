package com.sam.bluepad.data.crypto.files

import com.sam.bluepad.domain.crypto.files.CryptoFilePathProvider
import okio.Path
import okio.Path.Companion.toPath

actual class CryptoFilePathProviderImpl : CryptoFilePathProvider {

    override fun readCryptoDir(): Path {
        val home = System.getProperty("user.home")
        val appName = "bluepad"
        return home.toPath() / appName / "crypto"
    }
}
