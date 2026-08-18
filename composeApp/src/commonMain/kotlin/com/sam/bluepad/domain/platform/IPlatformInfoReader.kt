package com.sam.bluepad.domain.platform

fun interface IPlatformInfoReader {
    suspend fun readPlatform(): Result<PlatformDeviceInfo>
}
