package com.sam.bluepad.di

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.koin.dsl.module

actual fun createPlatformTestModule() = module {
    single<Context> { InstrumentationRegistry.getInstrumentation().context }
}

