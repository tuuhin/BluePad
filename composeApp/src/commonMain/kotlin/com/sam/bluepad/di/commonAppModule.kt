package com.sam.bluepad.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sam.bluepad.data.crypto.EncryptionSessionManagerImpl
import com.sam.bluepad.data.crypto.encryption.AESCBCEncryptionManager
import com.sam.bluepad.data.crypto.files.KeyFileManagerImpl
import com.sam.bluepad.data.crypto.files.SyncDiffFileManagerImpl
import com.sam.bluepad.data.database.AppDBBuilder
import com.sam.bluepad.data.database.BluePadDB
import com.sam.bluepad.data.datastore.DataStoreProvider
import com.sam.bluepad.data.datastore.DataStoreUtils
import com.sam.bluepad.data.datastore.LocalDeviceInfoProviderImpl
import com.sam.bluepad.data.repository.ExternalDevicesRepoImpl
import com.sam.bluepad.data.repository.SketchesRepoImpl
import com.sam.bluepad.data.serialization.SerializationProtocols
import com.sam.bluepad.data.sync.IncomingPayloadManagerImpl
import com.sam.bluepad.data.sync.OutgoingPayloadManagerImpl
import com.sam.bluepad.data.sync.SyncManagerImpl
import com.sam.bluepad.data.sync_diff.SyncDiffCalculatorImpl
import com.sam.bluepad.data.sync_diff.SyncDiffReviewManagerImpl
import com.sam.bluepad.domain.crypto.EncryptionManager
import com.sam.bluepad.domain.crypto.EncryptionSessionManager
import com.sam.bluepad.domain.crypto.KeyFileManager
import com.sam.bluepad.domain.crypto.SyncDiffFileManager
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.SyncManager
import com.sam.bluepad.domain.sync_diff.SyncDiffCalculator
import com.sam.bluepad.domain.sync_diff.SyncDiffReviewManager
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.domain.use_cases.HashGenerator
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.use_cases.RandomGeneratorImpl
import com.sam.bluepad.domain.use_cases.RandomNameGenerator
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val commonAppModule = module(true) {
    // db module
    single {
        val dbBuilder = get<AppDBBuilder>()
        BluePadDB.prepareRoomDb(dbBuilder.getDbBuilder())
    }
    single { get<BluePadDB>().devicesDao() }
    single { get<BluePadDB>().sketchesDao() }
    single { get<BluePadDB>().sketchMetadataDao() }
    single { get<BluePadDB>().sketchContentDao() }

    // preferences
    single(createdAtStart = true) { get<DataStoreProvider>().provideDataStore(DataStoreUtils.APP_COMMONS_DATASTORE_FILE) }
        .bind<DataStore<Preferences>>()

    //utils
    singleOf(::RandomNameGenerator)
    singleOf(::RandomGeneratorImpl) bind RandomGenerator::class
    singleOf(::HashGenerator)
    single { SerializationProtocols.protobuf } bind ProtoBuf::class
    single { BytesEncoder() } bind BytesEncoder::class

    // sync manager
    factoryOf(::SyncManagerImpl) bind SyncManager::class
    factoryOf(::OutgoingPayloadManagerImpl) bind OutPayloadManager::class
    factoryOf(::IncomingPayloadManagerImpl) bind InPayloadManager::class

    // device id provider
    singleOf(::LocalDeviceInfoProviderImpl) bind LocalDeviceInfoProvider::class
    // repository
    factoryOf(::ExternalDevicesRepoImpl) bind ExternalDevicesRepository::class
    factoryOf(::SketchesRepoImpl) bind SketchesRepository::class

    // sync diffs
    factoryOf(::SyncDiffCalculatorImpl) bind SyncDiffCalculator::class
    factoryOf(::SyncDiffReviewManagerImpl) bind SyncDiffReviewManager::class

    // files
    singleOf(::KeyFileManagerImpl) bind KeyFileManager::class
    factoryOf(::SyncDiffFileManagerImpl) bind SyncDiffFileManager::class

    // crypto
    factoryOf(::AESCBCEncryptionManager) bind EncryptionManager::class
    factoryOf(::EncryptionSessionManagerImpl)
    factoryOf(::EncryptionSessionManagerImpl) bind EncryptionSessionManager::class
}
