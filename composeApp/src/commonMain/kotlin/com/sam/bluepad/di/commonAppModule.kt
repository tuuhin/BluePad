package com.sam.bluepad.di

import com.sam.bluepad.data.crypto.EncryptionSessionManagerImpl
import com.sam.bluepad.data.crypto.encryption.AESCBCEncryptionManager
import com.sam.bluepad.data.crypto.files.KeyFileManagerImpl
import com.sam.bluepad.data.crypto.files.SyncDiffFileManagerImpl
import com.sam.bluepad.data.database.AppDBBuilder
import com.sam.bluepad.data.database.BluePadDB
import com.sam.bluepad.data.database.dao.DevicesInfoDao
import com.sam.bluepad.data.database.dao.SketchContentDao
import com.sam.bluepad.data.database.dao.SketchMetadataDao
import com.sam.bluepad.data.database.dao.SketchesDao
import com.sam.bluepad.data.datastore.DataStoreProvider
import com.sam.bluepad.data.datastore.LocalDeviceInfoProviderImpl
import com.sam.bluepad.data.datastore.UserAppSettingsProviderImpl
import com.sam.bluepad.data.repository.ExternalDevicesRepoImpl
import com.sam.bluepad.data.repository.SketchesRepoImpl
import com.sam.bluepad.data.serialization.SerializationProtocols
import com.sam.bluepad.data.sync.IncomingPayloadManagerImpl
import com.sam.bluepad.data.sync.OutgoingPayloadManagerImpl
import com.sam.bluepad.data.sync.SyncManagerImpl
import com.sam.bluepad.data.sync_diff.SyncDataSaverImpl
import com.sam.bluepad.data.sync_diff.SyncDataSessionReaderImpl
import com.sam.bluepad.data.sync_diff.SyncDiffCalculatorImpl
import com.sam.bluepad.domain.crypto.EncryptionManager
import com.sam.bluepad.domain.crypto.EncryptionSessionManager
import com.sam.bluepad.domain.crypto.KeyFileManager
import com.sam.bluepad.domain.crypto.SyncDiffFileManager
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.settings.UserAppSettingsProvider
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.SyncManager
import com.sam.bluepad.domain.sync_diff.SyncDataSaver
import com.sam.bluepad.domain.sync_diff.SyncDataSessionReader
import com.sam.bluepad.domain.sync_diff.SyncDiffCalculator
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.domain.use_cases.HashGenerator
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.use_cases.RandomGeneratorImpl
import com.sam.bluepad.domain.use_cases.RandomNameGenerator
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single

private fun provideRoomDb(builder: AppDBBuilder): BluePadDB = BluePadDB.prepareRoomDb(builder.getDbBuilder())
private fun provideDevicesDao(db: BluePadDB): DevicesInfoDao = db.devicesDao()
private fun provideSketchesDao(db: BluePadDB): SketchesDao = db.sketchesDao()
private fun provideSketchMetadataDao(db: BluePadDB): SketchMetadataDao = db.sketchMetadataDao()
private fun provideSketchContentDao(db: BluePadDB): SketchContentDao = db.sketchContentDao()
private fun provideProtoBuf(): ProtoBuf = SerializationProtocols.protobuf

val commonAppModule = module {
    // DB Module
    single(createdAtStart = true) { create(::provideRoomDb) } bind BluePadDB::class
    single { create(::provideDevicesDao) } bind DevicesInfoDao::class
    single { create(::provideSketchesDao) } bind SketchesDao::class
    single { create(::provideSketchMetadataDao) } bind SketchMetadataDao::class
    single { create(::provideSketchContentDao) } bind SketchContentDao::class

    // DataStore & Settings
    single<DataStoreProvider>()
    single<UserAppSettingsProviderImpl>() bind UserAppSettingsProvider::class

    // Utils
    single<RandomNameGenerator>()
    single<RandomGeneratorImpl>() bind RandomGenerator::class
    single<HashGenerator>()
    single { create(::provideProtoBuf) } bind ProtoBuf::class
    single<BytesEncoder>()

    // Sync Manager
    factory<SyncManagerImpl>() bind SyncManager::class
    factory<OutgoingPayloadManagerImpl> { create(::OutgoingPayloadManagerImpl) } bind OutPayloadManager::class
    factory<IncomingPayloadManagerImpl> { create(::IncomingPayloadManagerImpl) } bind InPayloadManager::class

    // Device Info Provider
    single<LocalDeviceInfoProviderImpl>() bind LocalDeviceInfoProvider::class

    // Repository
    factory<ExternalDevicesRepoImpl>() bind ExternalDevicesRepository::class
    factory<SketchesRepoImpl> { create(::SketchesRepoImpl) } bind SketchesRepository::class

    // Sync Diffs
    factory<SyncDiffCalculatorImpl>() bind SyncDiffCalculator::class
    factory<SyncDataSessionReaderImpl> { create(::SyncDataSessionReaderImpl) } bind SyncDataSessionReader::class
    factory<SyncDataSaverImpl>() bind SyncDataSaver::class

    // Files
    single<KeyFileManagerImpl>() bind KeyFileManager::class
    factory<SyncDiffFileManagerImpl>() bind SyncDiffFileManager::class

    // Crypto
    factory<AESCBCEncryptionManager> { create(::AESCBCEncryptionManager) } bind EncryptionManager::class
    factory<EncryptionSessionManagerImpl> { create(::EncryptionSessionManagerImpl) } bind EncryptionSessionManager::class
}
