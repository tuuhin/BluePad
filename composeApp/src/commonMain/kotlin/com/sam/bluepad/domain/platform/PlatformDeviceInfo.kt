package com.sam.bluepad.domain.platform

data class PlatformDeviceInfo(
    val osName: String? = null,
    val osVersion: String? = null,
    val buildNumber: String? = null,
    val arch: String? = null,
    val bluetoothAdapter: String? = null,
    val bluetoothVendor: String? = null,
    val macAddress: String? = null,
    val isMacAddressHidden: Boolean = false,
)
