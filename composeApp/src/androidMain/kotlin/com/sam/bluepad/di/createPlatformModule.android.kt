package com.sam.bluepad.di

import android.content.Context
import com.sam.bluepad.data.ble.BLEAdvertisementImpl
import com.sam.bluepad.data.ble.BLEConnectionManagerImpl
import com.sam.bluepad.data.ble.BLEDiscoveryImpl
import com.sam.bluepad.data.ble.BLEGattAdvertiserConfig
import com.sam.bluepad.data.ble.BLESyncConnectionManagerImpl
import com.sam.bluepad.data.ble.callbacks.BLEGattAdvertisementCallback
import com.sam.bluepad.data.ble.callbacks.ServerConnectionCallback
import com.sam.bluepad.data.ble.callbacks.SyncDeviceConnectionCallback
import com.sam.bluepad.data.ble.callbacks.SyncDeviceDiscoveryCallback
import com.sam.bluepad.data.bluetooth.BTDeviceBondManagerImpl
import com.sam.bluepad.data.bluetooth.BTEnableRequestProviderImpl
import com.sam.bluepad.data.bluetooth.BluetoothStateProviderImpl
import com.sam.bluepad.data.crypto.encryption.KeyEncryptionManagerImpl
import com.sam.bluepad.data.crypto.files.CryptoFilePathProviderImpl
import com.sam.bluepad.data.database.AppDBBuilder
import com.sam.bluepad.data.interactions.CopySketchInteractionImpl
import com.sam.bluepad.data.interactions.ShareSketchInteractionImpl
import com.sam.bluepad.data.utils.CommonAppFilesStore
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConnectionManager
import com.sam.bluepad.domain.ble.BLEDiscoveryManager
import com.sam.bluepad.domain.ble.BLESyncConnectionManager
import com.sam.bluepad.domain.bluetooth.BTDeviceBondManager
import com.sam.bluepad.domain.bluetooth.BTEnableRequestProvider
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import com.sam.bluepad.domain.crypto.KeyEncryptionManager
import com.sam.bluepad.domain.crypto.files.CryptoFilePathProvider
import com.sam.bluepad.domain.interactions.CopySketchInteraction
import com.sam.bluepad.domain.interactions.ShareSketchInteraction
import dev.icerock.moko.permissions.PermissionsController
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single

private fun createAppDb(context: Context): AppDBBuilder = AppDBBuilder(context)
private fun createPermissionController(context: Context): PermissionsController = PermissionsController(context)

actual fun createPlatformModule(): Module = module {

    single<CommonAppFilesStore> { create(::CommonAppFilesStore) }
    single<AppDBBuilder> { create(::createAppDb) }

    // coroutines provider
    single<PlatformDispatcherProvider>()

    // ble provider
    single<BLEDiscoveryImpl>() bind BLEDiscoveryManager::class
    single<BLEConnectionManagerImpl>() bind BLEConnectionManager::class

    // ble advertisement
    single<ServerConnectionCallback> { create(::ServerConnectionCallback) }
    factory<BLEGattAdvertisementCallback>()
    single<BLEGattAdvertiserConfig>()
    single<BLEAdvertisementImpl>() bind BLEAdvertisementManager::class

    // ble sync callbacks
    factory<SyncDeviceDiscoveryCallback>()
    factory<SyncDeviceConnectionCallback> { create(::SyncDeviceConnectionCallback) }
    single<BLESyncConnectionManagerImpl>() bind BLESyncConnectionManager::class

    // permission controller
    single<PermissionsController> { create(::createPermissionController) }

    // bluetooth state provider
    single<BluetoothStateProviderImpl>() bind BluetoothStateProvider::class
    factory<BTDeviceBondManagerImpl>() bind BTDeviceBondManager::class
    factory<BTEnableRequestProviderImpl>() bind BTEnableRequestProvider::class

    single<PlatformInfoProvider>()

    // interactions
    single<ShareSketchInteractionImpl>() bind ShareSketchInteraction::class
    single<CopySketchInteractionImpl>() bind CopySketchInteraction::class

    // crypto
    single<CryptoFilePathProviderImpl>() bind CryptoFilePathProvider::class
    factory<KeyEncryptionManagerImpl>() bind KeyEncryptionManager::class
}
