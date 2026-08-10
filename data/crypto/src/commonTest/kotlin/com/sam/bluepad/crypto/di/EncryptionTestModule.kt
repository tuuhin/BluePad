package com.sam.bluepad.crypto.di

import com.sam.bluepad.common.di.CommonModule
import com.sam.bluepad.common.utils.IFilesProvider
import com.sam.bluepad.testing.data.TestFileProvider
import com.sam.bluepad.testing.di.TestPlatformModule
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module(
    includes = [
        TestPlatformModule::class,
        CommonModule::class,
        CryptoModule::class,
    ],
)
class EncryptionTestModule {


    @Factory
    fun providesFileProvider(): IFilesProvider = TestFileProvider()
}
