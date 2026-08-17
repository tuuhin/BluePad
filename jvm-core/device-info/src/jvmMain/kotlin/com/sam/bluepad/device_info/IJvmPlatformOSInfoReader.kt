package com.sam.bluepad.com.sam.bluepad.device_info

import com.sam.bluepad.com.sam.bluepad.device_info.models.JvmPlatformOSInfo

interface IJvmPlatformOSInfoReader {
    suspend fun invoke(): JvmPlatformOSInfo
}
