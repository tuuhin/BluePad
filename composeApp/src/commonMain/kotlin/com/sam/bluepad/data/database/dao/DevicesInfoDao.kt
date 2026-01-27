package com.sam.bluepad.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.sam.bluepad.data.database.entities.DeviceInfoEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface DevicesInfoDao {

	@Upsert
	suspend fun insertOrUpdateDevice(entity: DeviceInfoEntity): Long

	@Upsert
	suspend fun insertOrUpdateDevices(entity: List<DeviceInfoEntity>)

	@Query("SELECT * FROM device_info_table WHERE is_revoked=:isRevoked")
	fun readAllDevices(isRevoked: Boolean): Flow<List<DeviceInfoEntity>>

	@Query("SELECT * from device_info_table WHERE device_id=:uuid")
	suspend fun readDeviceById(uuid: Uuid): DeviceInfoEntity?

	@Query("SELECT EXISTS(SELECT 1 from device_info_table WHERE device_id=:uuid)")
	suspend fun checkIfDeviceExists(uuid: Uuid): Boolean

	@Query("SELECT * from device_info_table WHERE device_id in (:uuids)")
	suspend fun readDevicesByIds(uuids: List<Uuid>): List<DeviceInfoEntity>

	@Query("UPDATE device_info_table SET is_revoked=false WHERE is_revoked=true")
	suspend fun reEnrollAllDevice(): Int

	@Query("UPDATE device_info_table SET is_revoked=false WHERE device_id=:uuid")
	suspend fun reEnrollDeviceByID(uuid: Uuid): Int

	@Query("UPDATE device_info_table SET is_revoked=:newRevokeStatus WHERE device_id=:deviceId")
	suspend fun setRevokeStatusOnDeviceByID(newRevokeStatus: Boolean, deviceId: Uuid)

	@Delete
	suspend fun deleteDevice(entity: DeviceInfoEntity)

	@Delete
	suspend fun deleteDevices(entities: List<DeviceInfoEntity>)
}