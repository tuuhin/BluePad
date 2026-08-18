package com.sam.bluepad.domain.use_cases

import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import kotlinx.coroutines.withContext
import org.kotlincrypto.random.CryptoRand

class RandomGeneratorImpl(
    private val dispatcherProvider: PlatformDispatcherProvider
) : RandomGenerator {

    override suspend fun generateRandomBytes(size: Int): ByteArray {
        return withContext(dispatcherProvider.default) { CryptoRand.nextBytes(ByteArray(size)) }
    }
}
