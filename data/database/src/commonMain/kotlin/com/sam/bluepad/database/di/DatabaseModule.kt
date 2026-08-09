package com.sam.bluepad.database.di

import com.sam.bluepad.database.data.AppDBBuilder
import com.sam.bluepad.database.data.BluePadDB
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module(
    includes = [DaoModule::class],
    createdAtStart = true,
)
@ComponentScan("com.sam.bluepad.database.data")
object DatabaseModule {


    @Singleton
    internal fun createAppDB(builder: AppDBBuilder): BluePadDB =
        BluePadDB.prepareRoomDb(builder.getDbBuilder())
}
