package com.sam.bluepad.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sam.bluepad.data.database.convertors.InstantTypeConvertor
import com.sam.bluepad.data.database.convertors.UUIDTypeConvertors
import com.sam.bluepad.data.database.entities.DeviceInfoEntity
import com.sam.bluepad.data.database.entities.SketchContentEntity
import com.sam.bluepad.data.database.entities.SketchMetadataEntity
import com.sam.bluepad.data.database.entities.SketchUpdateLogEntity
import kotlinx.coroutines.Dispatchers

@Database(
	entities = [
		DeviceInfoEntity::class,
		SketchMetadataEntity::class,
		SketchContentEntity::class,
		SketchUpdateLogEntity::class,
	],
	version = 1
)
@TypeConverters(
	value = [
		InstantTypeConvertor::class,
		UUIDTypeConvertors::class,
	]
)
@ConstructedBy(AppDBConstructor::class)
abstract class BluePadDB : RoomDatabase() {

	companion object {

		const val APP_DB_NAME = "bluepad_sketches.db"

		@Volatile
		private var _database: BluePadDB? = null

		private val _dbLock = Any()

		fun prepareRoomDb(builder: Builder<BluePadDB>): BluePadDB = synchronized(_dbLock) {
			if (_database == null) {
				builder
					.setDriver(BundledSQLiteDriver())
					.setQueryCoroutineContext(Dispatchers.IO)
					.build()
					.also { _database = it }
			}
			_database!!
		}

	}
}