package com.sam.bluepad

import android.app.Application
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
class BluePadApp : Application(), KoinStartup {

	override fun onKoinStartup(): KoinConfiguration = koinConfiguration {
		androidContext(this@BluePadApp)
		androidLogger()
		modules(createPlatformModule(), commonAppModule, viewModelModule)
	}

}