package com.sam.bluepad.crypto.data.files

import com.sam.bluepad.common.utils.IFilesProvider
import com.sam.bluepad.crypto.domain.ICryptoFilePathProvider
import okio.Path
import org.koin.core.annotation.Factory

@Factory(binds = [ICryptoFilePathProvider::class])
internal class CryptoFilePathProvider(private val filesStore: IFilesProvider) : ICryptoFilePathProvider {


    override fun readCryptoDir(): Path {
        return filesStore.cacheDirectory() / "crypto"
    }
}
