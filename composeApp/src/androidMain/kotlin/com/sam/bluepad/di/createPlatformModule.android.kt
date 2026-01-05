package com.sam.bluepad.di

import com.sam.bluepad.data.ble.BLEAdvertisementImpl
import com.sam.bluepad.data.ble.BLEDiscoveryImpl
import com.sam.bluepad.data.database.AppDBBuilder
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEDiscoveryManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createPlatformModule(): Module = module {
	single { AppDBBuilder(androidContext()) }
	single { BLEDiscoveryImpl(androidContext()) } bind BLEDiscoveryManager::class
	single { BLEAdvertisementImpl(androidContext()) } bind BLEAdvertisementManager::class
}
