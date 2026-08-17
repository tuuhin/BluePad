package com.sam.bluepad.device_info

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryGetTypeID
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFGetTypeID
import platform.CoreFoundation.CFPropertyListCreateWithStream
import platform.CoreFoundation.CFReadStreamClose
import platform.CoreFoundation.CFReadStreamCreateWithFile
import platform.CoreFoundation.CFReadStreamOpen
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringGetCString
import platform.CoreFoundation.CFStringGetLength
import platform.CoreFoundation.CFStringGetMaximumSizeForEncoding
import platform.CoreFoundation.CFStringGetTypeID
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFURLCreateWithFileSystemPath
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFPropertyListImmutable
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFURLPOSIXPathStyle
import platform.darwin.CTL_KERN
import platform.darwin.KERN_OSVERSION
import platform.darwin.sysctl
import platform.posix.size_tVar
import platform.posix.uname
import platform.posix.utsname

actual class PlatformOSInfoReader : IPlatformOSInfoReader {

    actual override val arch: String
        get() = memScoped {
            val sysInfo = alloc<utsname>()
            val re = uname(sysInfo.ptr)
            if (re != 0) return@memScoped "Unknown"
            sysInfo.machine.toKStringFromUtf8()
        }

    actual override val buildNumber: String
        get() = memScoped {
            val mib = allocArray<IntVar>(2)
            mib[0] = CTL_KERN
            mib[1] = KERN_OSVERSION

            val buildBuf = allocArray<ByteVar>(256)
            val buildLen = alloc<size_tVar>()
            buildLen.value = 256u

            val res = sysctl(mib, 2u, buildBuf, buildLen.ptr, null, 0u)
            if (res == 0) buildBuf.toKString() else "Unknown"
        }


    actual override val osName: String
        get() = readSystemPlistKey("ProductName") ?: readSysName()

    actual override val osVersion: String
        get() = readSystemPlistKey("ProductVersion") ?: "Unknown"


    private fun readSysName() = memScoped {
        val sysInfo = alloc<utsname>()
        val re = uname(sysInfo.ptr)
        if (re != 0) return@memScoped "Unknown"
        sysInfo.sysname.toKStringFromUtf8()
    }

    private fun readSystemPlistKey(key: String): String? = memScoped {
        val plistPath = "/System/Library/CoreServices/SystemVersion.plist"

        val cfPath = CFStringCreateWithCString(kCFAllocatorDefault, plistPath, kCFStringEncodingUTF8)
            ?: return@memScoped null

        val url = try {
            CFURLCreateWithFileSystemPath(kCFAllocatorDefault, cfPath, kCFURLPOSIXPathStyle, false)
                ?: return@memScoped null
        } finally {
            CFRelease(cfPath)
        }

        val stream = CFReadStreamCreateWithFile(kCFAllocatorDefault, url)
        CFRelease(url)

        if (stream == null) return null

        try {
            if (!CFReadStreamOpen(stream)) return null

            val plist =
                CFPropertyListCreateWithStream(kCFAllocatorDefault, stream, 0, kCFPropertyListImmutable, null, null)
                    ?: return@memScoped null
            try {
                if (CFGetTypeID(plist) != CFDictionaryGetTypeID()) return null

                val cfKey = CFStringCreateWithCString(kCFAllocatorDefault, key, kCFStringEncodingUTF8)
                    ?: return@memScoped null

                try {
                    val dictRef: CFDictionaryRef = plist.reinterpret()
                    val valueRef = CFDictionaryGetValue(dictRef, cfKey) ?: return@memScoped null
                    if (CFGetTypeID(valueRef) != CFStringGetTypeID()) return@memScoped null

                    val cfValue: CFStringRef = valueRef.reinterpret()
                    val length = CFStringGetLength(cfValue)
                    val maxSize = CFStringGetMaximumSizeForEncoding(length, kCFStringEncodingUTF8) + 1
                    val buffer = allocArray<ByteVar>(maxSize)

                    if (!CFStringGetCString(cfValue, buffer, maxSize, kCFStringEncodingUTF8))
                        return@memScoped null
                    return buffer.toKStringFromUtf8()
                } finally {
                    CFRelease(cfKey)
                }
            } finally {
                CFRelease(plist)
            }
        } finally {
            CFReadStreamClose(stream)
            CFRelease(stream)
        }
    }
}
