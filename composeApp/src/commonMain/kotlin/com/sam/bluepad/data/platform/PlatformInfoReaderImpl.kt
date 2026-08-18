package com.sam.bluepad.data.platform

import com.sam.bluepad.domain.platform.IPlatformInfoReader
import com.sam.bluepad.domain.platform.PlatformDeviceInfo

expect class PlatformInfoReaderImpl : IPlatformInfoReader {
    override suspend fun readPlatform(): Result<PlatformDeviceInfo>
}
