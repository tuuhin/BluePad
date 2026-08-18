package com.sam.bluepad.com.sam.bluepad.device_info

import com.sam.bluepad.com.sam.bluepad.device_info.models.JvmDeviceBTAdapterInfo

fun interface IJvmDeviceInfoReader {
    suspend fun readDevice(): JvmDeviceBTAdapterInfo
}
