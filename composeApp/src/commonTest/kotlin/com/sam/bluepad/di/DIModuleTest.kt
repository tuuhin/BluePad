package com.sam.bluepad.di

import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test
import kotlin.uuid.Uuid

@OptIn(KoinExperimentalAPI::class)
class DIModuleTest {

    private val appModule = module {
        includes(commonAppModule)
        includes(createPlatformModule())
        includes(viewModelModule)
    }

    @Test
    fun check_all_modules_present() {
        appModule.verify(extraTypes = listOf(Uuid::class))
    }
}
