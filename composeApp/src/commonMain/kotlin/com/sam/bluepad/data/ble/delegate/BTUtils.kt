package com.sam.bluepad.data.ble.delegate

private val ENABLE_NOTIFICATION_VALUE = byteArrayOf(0x1, 0x0)
private val ENABLE_INDICATION_VALUE = byteArrayOf(0x2, 0x0)
private val DISABLE_NOTIFICATION_VALUE = byteArrayOf(0x0, 0x0)

val ByteArray.btDescriptorsNotificationOrIndicationEnabled: Boolean
    get() = when {
        contentEquals(ENABLE_NOTIFICATION_VALUE) -> true
        contentEquals(ENABLE_INDICATION_VALUE) -> true
        contentEquals(DISABLE_NOTIFICATION_VALUE) -> false
        else -> throw IllegalArgumentException("CCC descriptor values are fixed cannot use :${toHexString()}")
    }

fun Boolean.asCCCDescriptorValue(isIndication: Boolean): ByteArray {
    return when (this) {
        true if (isIndication) -> ENABLE_INDICATION_VALUE
        true -> ENABLE_NOTIFICATION_VALUE
        false -> DISABLE_NOTIFICATION_VALUE
    }
}

val ByteArray.isCCCDescriptorEnabled: Boolean
    get() = contentEquals(ENABLE_INDICATION_VALUE) || contentEquals(ENABLE_NOTIFICATION_VALUE)
