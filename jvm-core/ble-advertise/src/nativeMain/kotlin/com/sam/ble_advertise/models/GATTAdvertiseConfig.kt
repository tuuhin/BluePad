package com.sam.ble_advertise.models

import kotlinx.cinterop.toKString

class GATTAdvertiseConfig internal constructor(
    val discoverable: Boolean = true,
    val connectable: Boolean = true,
    val serviceData: String = ""
) {
    constructor(
        discoverable: Boolean = true,
        connectable: Boolean = true,
        serviceData: ByteArray
    ) : this(discoverable = discoverable, connectable = connectable, serviceData = serviceData.toKString())
}
