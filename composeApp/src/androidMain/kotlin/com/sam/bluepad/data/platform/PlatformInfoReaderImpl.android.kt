package com.sam.bluepad.data.platform

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.hasConnectPermission
import com.sam.bluepad.domain.platform.IPlatformInfoReader
import com.sam.bluepad.domain.platform.PlatformDeviceInfo

private const val TAG = "ANDROID_DEVICE_INFO"

actual class PlatformInfoReaderImpl(private val context: Context) : IPlatformInfoReader {

    private val bluetoothAdapter by lazy { context.getSystemService<BluetoothManager>() }

    actual override suspend fun readPlatform(): Result<PlatformDeviceInfo> {
        val osName = "Android"
        val osVersion = Build.VERSION.RELEASE ?: "Unknown"
        val buildNumber = Build.DISPLAY ?: Build.ID
        val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: System.getProperty("os.arch") ?: "Unknown"

        val adapter = bluetoothAdapter?.adapter

        return runCatching {

            try {
                val name = if (context.hasConnectPermission) adapter?.name else null
                val mac = if (context.hasConnectPermission) adapter?.address else null

                PlatformDeviceInfo(
                    osName = osName,
                    osVersion = osVersion,
                    buildNumber = buildNumber,
                    arch = arch,
                    bluetoothAdapter = name,
                    bluetoothVendor = Build.MANUFACTURER,
                    macAddress = mac,
                    isMacAddressHidden = true,
                )
            } catch (e: SecurityException) {
                Logger.e(e, TAG) { "FAILED TO READ BLUETOOTH DATA" }
                PlatformDeviceInfo(
                    osName = osName,
                    osVersion = osVersion,
                    buildNumber = buildNumber,
                    arch = arch,
                    bluetoothAdapter = null,
                    bluetoothVendor = Build.MANUFACTURER,
                    macAddress = null,
                    isMacAddressHidden = true,
                )
            }
        }
    }
}
