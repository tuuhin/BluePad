package com.sam.bluepad.di

import com.sam.bluepad.data.crypto.TestCryptoFileProvider
import com.sam.bluepad.data.database.AppDBBuilder
import com.sam.bluepad.data.database.BluePadDB
import com.sam.bluepad.data.repository.FakeSketchesRepoImpl
import com.sam.bluepad.domain.crypto.files.CryptoFilePathProvider
import com.sam.bluepad.domain.repository.SketchesRepository
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.single

private fun provideRoomDb(builder: AppDBBuilder): BluePadDB = BluePadDB.prepareRoomDb(builder.getMemoryDbBuilder())

val testModule = module {
    single(createdAtStart = true) { create(::provideRoomDb) } bind BluePadDB::class
    single<TestCryptoFileProvider>() bind CryptoFilePathProvider::class
    single<FakeSketchesRepoImpl>() bind SketchesRepository::class
}
