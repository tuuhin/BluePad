package com.sam.bluepad.data.crypto.files

import android.content.Context
import com.sam.bluepad.domain.crypto.files.CryptoFilePathProvider
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

actual class CryptoFilePathProviderImpl(private val context: Context) : CryptoFilePathProvider {

    override fun readCryptoDir(): Path {
        val file = File(context.filesDir, "crypto").apply { mkdirs() }
        return file.toOkioPath()
    }
}
