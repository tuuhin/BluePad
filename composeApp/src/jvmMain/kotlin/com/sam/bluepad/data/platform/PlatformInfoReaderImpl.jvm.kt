package com.sam.bluepad.data.platform

import com.sam.bluepad.com.sam.bluepad.device_info.JVMDeviceInfoReader
import com.sam.bluepad.com.sam.bluepad.device_info.JVMPlatformInfoReader
import com.sam.bluepad.domain.platform.IPlatformInfoReader
import com.sam.bluepad.domain.platform.PlatformDeviceInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

actual class PlatformInfoReaderImpl : IPlatformInfoReader {

    private val osInfo by lazy { JVMPlatformInfoReader() }
    private val btAdapterInfo by lazy { JVMDeviceInfoReader() }

    actual override suspend fun readPlatform(): Result<PlatformDeviceInfo> {
        return coroutineScope {
            try {
                val osAboutDeferred = async { osInfo.invoke() }
                val btAboutDeferred = async { btAdapterInfo.readDevice() }

                val osAbout = osAboutDeferred.await()
                val btAbout = btAboutDeferred.await()

                val device = PlatformDeviceInfo(
                    osName = osAbout.osName,
                    osVersion = osAbout.osVersion,
                    buildNumber = osAbout.buildNumber,
                    arch = osAbout.arch,
                    bluetoothAdapter = btAbout.bluetoothAdapter,
                    bluetoothVendor = btAbout.bluetoothVendor,
                    macAddress = btAbout.macAddress,
                )
                Result.success(device)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }
}
