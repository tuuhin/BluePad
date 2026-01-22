package com.sam.bluepad.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sam.bluepad.data.database.AppDBBuilder
import com.sam.bluepad.data.database.BluePadDB
import com.sam.bluepad.data.datastore.DataStoreProvider
import com.sam.bluepad.data.datastore.DataStoreUtils
import com.sam.bluepad.data.datastore.LocalDeviceInfoProviderImpl
import com.sam.bluepad.data.repository.ExternalDevicesRepoImpl
import com.sam.bluepad.data.repository.SketchesRepoImpl
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.use_cases.HashGenerator
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.use_cases.RandomGeneratorImpl
import com.sam.bluepad.domain.use_cases.RandomNameGenerator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

@OptIn(ExperimentalSerializationApi::class)
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
	single { ProtoBuf } bind ProtoBuf::class

	// device id provider
	singleOf(::LocalDeviceInfoProviderImpl) bind LocalDeviceInfoProvider::class
	// repository
	factoryOf(::ExternalDevicesRepoImpl) bind ExternalDevicesRepository::class
	factoryOf(::SketchesRepoImpl) bind SketchesRepository::class
}