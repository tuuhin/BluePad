package com.sam.preferences.di

import com.sam.bluepad.common.shared.RandomNameGenerator
import com.sam.bluepad.common.utils.IFilesProvider
import com.sam.bluepad.testing.data.TestFileProvider
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
internal class TestCommonModule {


    @Factory
    fun randomNameProvider(): RandomNameGenerator = RandomNameGenerator()

    @Factory
    fun providesFileProvider(): IFilesProvider = TestFileProvider()
}
