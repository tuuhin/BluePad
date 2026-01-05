package com.sam.bluepad.data.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

val Context.hasBLEScanPermission: Boolean
	get() = ContextCompat.checkSelfPermission(
		this,
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			Manifest.permission.BLUETOOTH_SCAN
		else Manifest.permission.BLUETOOTH
	) == PermissionChecker.PERMISSION_GRANTED

val Context.hasBLEFeature: Boolean
	get() = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

val Context.hasLocationPermission: Boolean
	get() = ContextCompat.checkSelfPermission(
		this,
		Manifest.permission.ACCESS_COARSE_LOCATION
	) == PermissionChecker.PERMISSION_GRANTED

val Context.hasConnectPermission: Boolean
	get() = ContextCompat.checkSelfPermission(
		this,
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			Manifest.permission.BLUETOOTH_CONNECT
		else Manifest.permission.BLUETOOTH
	) == PermissionChecker.PERMISSION_GRANTED

val Context.hasAdvertisePermission: Boolean
	get() = ContextCompat.checkSelfPermission(
		this,
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			Manifest.permission.BLUETOOTH_ADVERTISE
		else Manifest.permission.BLUETOOTH
	) == PermissionChecker.PERMISSION_GRANTED