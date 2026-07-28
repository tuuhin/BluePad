package com.sam.bluepad.data.crypto.files

import com.sam.bluepad.data.utils.CommonAppFilesStore
import com.sam.bluepad.domain.crypto.files.CryptoFilePathProvider
import okio.Path

class CryptoFilePathProviderImpl(
    private val filesStore: CommonAppFilesStore
) : CryptoFilePathProvider {

    override fun readCryptoDir(): Path {
        return filesStore.cacheDirectory() / "crypto"
    }
}
