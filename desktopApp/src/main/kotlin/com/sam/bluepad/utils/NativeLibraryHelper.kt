package com.sam.bluepad.utils

import co.touchlab.kermit.Logger

internal fun setupNativeLibraries() {
    val osName = System.getProperty("os.name")

    if (!osName.startsWith("win", true)) return

    val thLocal = ThreadLocal.withInitial {
        System.loadLibrary("bt_common")
        System.loadLibrary("ble_advertise")
    }

    try {
        thLocal.get()
        Logger.w(tag = "NATIVE_LIB") { "LIBRARIES LINKED SUCCESSFULLY" }
    } catch (e: UnsatisfiedLinkError) {
        Logger.w(tag = "NATIVE_LIB", throwable = e) { "UNABLE TO LINK LIBRARIES" }
    } finally {
        thLocal.remove()
    }

}
