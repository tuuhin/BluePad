package com.sam.bluepad.com.sam.bluepad.device_info

fun interface IJvmDeviceInfoReader {
    suspend fun readDevice(): JvmDeviceInfo
}
