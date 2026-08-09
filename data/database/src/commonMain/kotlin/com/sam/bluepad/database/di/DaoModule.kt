package com.sam.bluepad.database.di

import com.sam.bluepad.database.data.BluePadDB
import com.sam.bluepad.database.data.dao.DevicesInfoDao
import com.sam.bluepad.database.data.dao.SketchContentDao
import com.sam.bluepad.database.data.dao.SketchMetadataDao
import com.sam.bluepad.database.data.dao.SketchesDao
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
internal object DaoModule {


    @Singleton
    fun provideDevicesDao(db: BluePadDB): DevicesInfoDao = db.devicesDao()

    @Singleton
    fun providesSketchContentDao(db: BluePadDB): SketchContentDao = db.sketchContentDao()

    @Singleton
    fun providesSketchesDao(db: BluePadDB): SketchesDao = db.sketchesDao()

    @Singleton
    fun providesMetadataDao(db: BluePadDB): SketchMetadataDao = db.sketchMetadataDao()
}
