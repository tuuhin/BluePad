package com.sam.bluepad.data.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "sketch_content_table",
    foreignKeys = [
        ForeignKey(
            entity = SketchMetadataEntity::class,
            parentColumns = ["_id"],
            childColumns = ["_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("last_modified_by_device"),
    ],
)
data class SketchContentEntity(
    @ColumnInfo(name = "_id")
    @PrimaryKey(autoGenerate = false)
    val id: Uuid,

    @ColumnInfo(name = "content")
    val content: String = "",

    @ColumnInfo(name = "content_hash")
    val contentHash: String = "",

    @ColumnInfo(name = "last_modified")
    val modifiedAt: Instant = Clock.System.now(),

    @ColumnInfo(name = "last_modified_by_device")
    val modifiedByDeviceId: Uuid? = null,
)
