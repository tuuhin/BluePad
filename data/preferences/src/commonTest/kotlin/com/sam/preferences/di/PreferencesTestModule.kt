package com.sam.preferences.di

import com.sam.bluepad.common.di.CommonModule
import com.sam.bluepad.testing.di.TestPlatformModule
import org.koin.core.annotation.Module

@Module(
    includes = [
        TestPlatformModule::class,
        CommonModule::class,
        TestCommonModule::class,
        PreferencesModule::class,
    ],
)
class PreferencesTestModule
