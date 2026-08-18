package com.sam.bluepad.data.platform

import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.platform.IPlatformInfoReader
import com.sam.bluepad.domain.platform.PlatformDeviceInfo

expect class PlatformInfoReaderImpl : IPlatformInfoReader {

    override val platformOS: DevicePlatformOS

    override suspend fun readPlatform(): Result<PlatformDeviceInfo>
}
