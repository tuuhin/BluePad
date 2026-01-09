package com.sam.bluepad.di

import com.sam.bluepad.data.ble.BLEAdvertisementImpl
import com.sam.bluepad.data.ble.BLEConnectionManagerImpl
import com.sam.bluepad.data.ble.BLEDiscoveryImpl
import com.sam.bluepad.data.bluetooth.BluetoothStateProviderImpl
import com.sam.bluepad.data.database.AppDBBuilder
import com.sam.bluepad.data.datastore.DataStoreProvider
import com.sam.bluepad.data.utils.JVMPermissionController
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConnectionManager
import com.sam.bluepad.domain.ble.BLEDiscoveryManager
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import com.sam.bluepad.domain.utils.PlatformInfoProvider
import dev.icerock.moko.permissions.PermissionsController
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createPlatformModule(): Module = module {
	//db
	single { AppDBBuilder() }
	// datastore
	singleOf(::DataStoreProvider)

	//ble
	singleOf(::BLEDiscoveryImpl) bind BLEDiscoveryManager::class
	singleOf(::BLEConnectionManagerImpl) bind BLEConnectionManager::class
	singleOf(::BLEAdvertisementImpl) bind BLEAdvertisementManager::class

	// permission controller
	single { JVMPermissionController() } bind PermissionsController::class
	// bluetooth state provider
	singleOf(::BluetoothStateProviderImpl) bind BluetoothStateProvider::class
	singleOf(::PlatformInfoProvider)
}