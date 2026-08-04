package com.sam.bluepad.utils

import co.touchlab.kermit.Logger
import dev.nucleusframework.core.runtime.Platform

internal fun setupNativeLibraries() {

    if (Platform.Current != Platform.Windows) return

    val thLocal = ThreadLocal.withInitial {
        System.loadLibrary("bt_common")
        System.loadLibrary("ble_advertise")
        System.loadLibrary("share_sheet")
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
