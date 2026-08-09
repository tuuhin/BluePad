package com.sam.bluepad.database.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sam.bluepad.common.models.PlatformOS
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "device_info_table",
    indices = [Index("is_revoked")],
)
data class DeviceInfoEntity(
    @ColumnInfo(name = "device_id")
    @PrimaryKey(autoGenerate = false)
    val id: Uuid,
    @ColumnInfo(name = "display_name")
    val displayName: String? = null,
    @ColumnInfo(name = "paired_at")
    val pairedAt: Instant,
    @ColumnInfo(name = "last_seen")
    val lastSeenAt: Instant,
    @ColumnInfo(name = "is_revoked")
    val isRevoked: Boolean = false,
    @ColumnInfo(name = "device_os", defaultValue = "UNKNOWN")
    val deviceOs: PlatformOS = PlatformOS.UNKNOWN,
)
