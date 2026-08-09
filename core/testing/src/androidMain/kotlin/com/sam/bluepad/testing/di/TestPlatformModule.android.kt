package com.sam.bluepad.testing.di

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
actual class TestPlatformModule {


    @Singleton
    fun provideTestContext(): Context {
        return InstrumentationRegistry.getInstrumentation().context
    }
}
