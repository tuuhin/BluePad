package com.sam.bluepad.di

import com.sam.bluepad.data.database.AppDBBuilder
import com.sam.bluepad.data.database.BluePadDB
import com.sam.bluepad.data.datastore.LocalDeviceInfoProviderImpl
import com.sam.bluepad.data.repository.ExternalDevicesRepoImpl
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.use_cases.RandomGeneratorImpl
import com.sam.bluepad.domain.use_cases.RandomNameGenerator
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
	//utils
	singleOf(::RandomNameGenerator)
	singleOf(::RandomGeneratorImpl) bind RandomGenerator::class

	// device id provider
	singleOf(::LocalDeviceInfoProviderImpl) bind LocalDeviceInfoProvider::class
	// repository
	factoryOf(::ExternalDevicesRepoImpl) bind ExternalDevicesRepository::class
}