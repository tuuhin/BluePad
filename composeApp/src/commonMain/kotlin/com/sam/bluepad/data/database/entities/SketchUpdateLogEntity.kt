package com.sam.bluepad.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sam.bluepad.domain.models.SketchChangeType
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
	tableName = "sketch_logs_entity",
	foreignKeys = [
		ForeignKey(
			entity = SketchMetadataEntity::class,
			parentColumns = ["_id"],
			childColumns = ["sketch_id"],
			onDelete = ForeignKey.CASCADE,
			onUpdate = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = DeviceInfoEntity::class,
			parentColumns = ["device_id"],
			childColumns = ["changed_by_device"],
			onDelete = ForeignKey.SET_NULL
		)
	],
	indices = [
		Index("sketch_id"),
		Index("changed_by_device"),
	],
)
data class SketchUpdateLogEntity(
	@ColumnInfo(name = "_id")
	@PrimaryKey(autoGenerate = false) val id: Uuid,

	@ColumnInfo(name = "sketch_id")
	val sketchId: Uuid,

	@ColumnInfo(name = "change_type")
	val changeType: SketchChangeType,

	@ColumnInfo(name = "prev_version")
	val prevVersion: Int = 0,

	@ColumnInfo(name = "new_version")
	val newVersion: Int = 0,

	@ColumnInfo(name = "modified_at")
	val modifiedAt: Instant = Clock.System.now(),

	@ColumnInfo(name = "changed_by_device")
	val deviceId: Uuid? = null,
)