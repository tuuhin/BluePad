package com.sam.bluepad.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
	tableName = "sketch_metadata_table",
	foreignKeys = [
		ForeignKey(
			entity = DeviceInfoEntity::class,
			parentColumns = ["device_id"],
			childColumns = ["created_by_device"],
			onDelete = ForeignKey.NO_ACTION,
		),
		ForeignKey(
			entity = DeviceInfoEntity::class,
			parentColumns = ["device_id"],
			childColumns = ["last_modified_by_device"],
			onDelete = ForeignKey.NO_ACTION
		)
	],
	indices = [
		Index("created_by_device"),
		Index("last_modified_by_device"),
	],
)
data class SketchMetadataEntity(
	@ColumnInfo(name = "_id")
	@PrimaryKey(autoGenerate = false) val id: Uuid,

	@ColumnInfo(name = "title")
	val title: String,

	@ColumnInfo(name = "created_at")
	val createdAt: Instant = Clock.System.now(),

	@ColumnInfo(name = "modified_at")
	val modifiedAt: Instant = Clock.System.now(),

	@ColumnInfo(name = "current_version")
	val version: Int = 0,

	@ColumnInfo(name = "is_deleted")
	val isDeleted: Boolean = false,

	@ColumnInfo(name = "created_by_device")
	val createdByDeviceId: Uuid? = null,

	@ColumnInfo(name = "last_modified_by_device")
	val lastModifiedByDevice: Uuid? = null
)