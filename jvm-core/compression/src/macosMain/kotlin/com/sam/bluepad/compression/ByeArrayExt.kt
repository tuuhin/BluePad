package com.sam.bluepad.compression

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.posix.memcpy

internal fun ByteArray.toNSData(): NSData {
    if (this.isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}

internal fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    if (size == 0) return ByteArray(0)
    val result = ByteArray(size)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}
