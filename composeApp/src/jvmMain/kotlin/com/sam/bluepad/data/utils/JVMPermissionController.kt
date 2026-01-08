package com.sam.bluepad.data.utils

import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController

class JVMPermissionController : PermissionsController {
	override suspend fun getPermissionState(permission: Permission): PermissionState =
		PermissionState.Granted

	override suspend fun isPermissionGranted(permission: Permission): Boolean = true

	override fun openAppSettings() = Unit

	override suspend fun providePermission(permission: Permission) = Unit
}