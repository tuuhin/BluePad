package com.sam.bluepad.data.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sam.bluepad.data.database.convertors.InstantTypeConvertor
import com.sam.bluepad.data.database.convertors.UUIDTypeConvertors
import com.sam.bluepad.data.database.dao.DevicesInfoDao
import com.sam.bluepad.data.database.dao.SketchContentDao
import com.sam.bluepad.data.database.dao.SketchMetadataDao
import com.sam.bluepad.data.database.dao.SketchesDao
import com.sam.bluepad.data.database.entities.DeviceInfoEntity
import com.sam.bluepad.data.database.entities.SketchAuditLogEntity
import com.sam.bluepad.data.database.entities.SketchContentEntity
import com.sam.bluepad.data.database.entities.SketchMetadataEntity
import kotlinx.coroutines.Dispatchers

@Database(
	entities = [
		DeviceInfoEntity::class,
		SketchMetadataEntity::class,
		SketchContentEntity::class,
		SketchAuditLogEntity::class,
	],
	version = 3,
	autoMigrations = [
		AutoMigration(from = 1, to = 2),
		AutoMigration(from = 2, to = 3),
	]
)
@TypeConverters(
	value = [
		InstantTypeConvertor::class,
		UUIDTypeConvertors::class,
	]
)
@ConstructedBy(AppDBConstructor::class)
abstract class BluePadDB : RoomDatabase() {

	abstract fun devicesDao(): DevicesInfoDao
	abstract fun sketchesDao(): SketchesDao
	abstract fun sketchMetadataDao(): SketchMetadataDao
	abstract fun sketchContentDao(): SketchContentDao

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