package com.sam.preferences.di

import com.sam.bluepad.common.di.CommonModule
import org.koin.core.annotation.Module

@Module(
    includes = [
        CommonModule::class,
        TestCommonModule::class,
        PreferencesModule::class,
    ],
)
class PreferencesTestModule
