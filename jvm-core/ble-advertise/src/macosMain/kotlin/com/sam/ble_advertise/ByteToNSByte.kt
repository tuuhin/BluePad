@file:OptIn(BetaInteropApi::class)

package com.sam.ble_advertise

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.data
import platform.posix.memcpy

internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData.data()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

internal fun NSData.toByteArray(): ByteArray {
    val length = length.toInt()
    if (length == 0) return byteArrayOf()
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes(), length.toULong())
    }
    return bytes
}
