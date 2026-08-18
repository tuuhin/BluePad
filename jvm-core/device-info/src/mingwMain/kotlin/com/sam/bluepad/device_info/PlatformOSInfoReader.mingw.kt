package com.sam.bluepad.device_info

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.windows.DWORDVar
import platform.windows.ERROR_SUCCESS
import platform.windows.GetModuleHandleW
import platform.windows.GetNativeSystemInfo
import platform.windows.GetProcAddress
import platform.windows.HKEY
import platform.windows.HKEYVar
import platform.windows.HKEY_LOCAL_MACHINE
import platform.windows.INT32
import platform.windows.KEY_READ
import platform.windows.PROCESSOR_ARCHITECTURE_AMD64
import platform.windows.PROCESSOR_ARCHITECTURE_ARM
import platform.windows.PROCESSOR_ARCHITECTURE_ARM64
import platform.windows.PROCESSOR_ARCHITECTURE_INTEL
import platform.windows.RTL_OSVERSIONINFOW
import platform.windows.RegCloseKey
import platform.windows.RegOpenKeyExW
import platform.windows.RegQueryValueExW
import platform.windows.SYSTEM_INFO
import platform.windows.USHORTVar

actual class PlatformOSInfoReader : IPlatformOSInfoReader {

    actual override val arch: String
        get() = memScoped {
            val sysInfo = alloc<SYSTEM_INFO>()
            GetNativeSystemInfo(sysInfo.ptr)
            when (sysInfo.wProcessorArchitecture.toInt()) {
                PROCESSOR_ARCHITECTURE_AMD64 -> "x64"
                PROCESSOR_ARCHITECTURE_ARM64 -> "ARM64"
                PROCESSOR_ARCHITECTURE_INTEL -> "x86"
                PROCESSOR_ARCHITECTURE_ARM -> "ARM"
                else -> "Unknown"
            }
        }

    actual override val osName: String
        get() = getRegString("ProductName") ?: "Windows"

    actual override val osVersion: String
        get() = getRegString("DisplayVersion") ?: "Unknown"

    actual override val buildNumber: String
        get() = memScoped {
            val hNtdll = GetModuleHandleW("ntdll.dll") ?: return@memScoped "Unknown"
            val pRtlGetVersion = GetProcAddress(hNtdll, "RtlGetVersion") ?: return@memScoped "Unknown"

            val rtlGetVersionFunc = pRtlGetVersion.reinterpret<CFunction<(CPointer<RTL_OSVERSIONINFOW>) -> INT32>>()
            val rovi = alloc<RTL_OSVERSIONINFOW>().apply {
                dwOSVersionInfoSize = sizeOf<RTL_OSVERSIONINFOW>().toUInt()
            }

            if (rtlGetVersionFunc(rovi.ptr) == 0) {
                val baseBuild = rovi.dwBuildNumber
                val ubr = getRegDword("UBR")
                if (ubr != null) "$baseBuild.$ubr" else "$baseBuild"
            } else {
                "Unknown"
            }
        }

    private fun getRegString(valueName: String): String? = memScoped {
        val hKey = openCurrentVersionKey() ?: return null
        val bufferSize = alloc<DWORDVar>().apply { value = 512.toUInt() }
        val buffer = allocArray<USHORTVar>(256)

        val result = RegQueryValueExW(hKey, valueName, null, null, buffer.reinterpret(), bufferSize.ptr)
        RegCloseKey(hKey)

        if (result == ERROR_SUCCESS) buffer.toKString() else null
    }

    private fun getRegDword(valueName: String): UInt? = memScoped {
        val hKey = openCurrentVersionKey() ?: return null
        val dwordVar = alloc<DWORDVar>()
        val dataSize = alloc<DWORDVar>().apply { value = sizeOf<DWORDVar>().toUInt() }

        val result = RegQueryValueExW(hKey, valueName, null, null, dwordVar.ptr.reinterpret(), dataSize.ptr)
        RegCloseKey(hKey)
        if (result == ERROR_SUCCESS) dwordVar.value else null
    }

    private fun openCurrentVersionKey(): HKEY? = memScoped {
        val hKeyVar = alloc<HKEYVar>()
        val subKey = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion"
        val status = RegOpenKeyExW(HKEY_LOCAL_MACHINE, subKey, 0.toUInt(), KEY_READ.toUInt(), hKeyVar.ptr)
        if (status == ERROR_SUCCESS) hKeyVar.value else null
    }
}
