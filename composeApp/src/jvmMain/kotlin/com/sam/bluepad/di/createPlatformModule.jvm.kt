package com.sam.bluepad.di

import com.sam.bluepad.data.ble.BLEAdvertisementImpl
import com.sam.bluepad.data.ble.BLEDiscoveryImpl
import com.sam.bluepad.data.database.AppDBBuilder
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEDiscoveryManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createPlatformModule(): Module = module {
	single { AppDBBuilder() }
	singleOf(::BLEDiscoveryImpl) bind BLEDiscoveryManager::class
	singleOf(::BLEAdvertisementImpl) bind BLEAdvertisementManager::class
}