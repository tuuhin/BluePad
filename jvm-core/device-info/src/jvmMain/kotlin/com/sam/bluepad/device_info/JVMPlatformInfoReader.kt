package com.sam.bluepad.com.sam.bluepad.device_info

import com.sam.bluepad.com.sam.bluepad.device_info.models.JvmPlatformOSInfo
import com.sam.bluepad.platform.device_info.PlatformOSInfoReader

class JVMPlatformInfoReader : IJvmPlatformOSInfoReader {

    override suspend fun invoke(): JvmPlatformOSInfo {
        val provider = PlatformOSInfoReader()
        return provider.use {
            JvmPlatformOSInfo(
                osName = it.osName,
                osVersion = it.osVersion,
                buildNumber = it.buildNumber,
                arch = it.arch,
            )
        }
    }
}
