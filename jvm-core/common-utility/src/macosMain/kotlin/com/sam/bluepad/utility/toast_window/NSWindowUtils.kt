package com.sam.bluepad.utility.toast_window

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretObjCPointerOrNull
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCPointer
import platform.AppKit.NSColor
import platform.AppKit.NSTextField
import platform.AppKit.NSView
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_getAssociatedObject
import platform.objc.objc_setAssociatedObject
import platform.posix.intptr_tVar

internal object ViewKeys {
    val LABEL = nativeHeap.alloc<ByteVar>()
}

internal fun saveViewRefs(root: NSView, label: NSTextField) {
    objc_setAssociatedObject(root, ViewKeys.LABEL.ptr, label, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
}

internal fun NSView.getLabel(): NSTextField? =
    objc_getAssociatedObject(this, ViewKeys.LABEL.ptr) as? NSTextField

internal fun Long.toNSView(): NSView? {
    try {
        if (this == 0L || this == -1L) return null
        val pointer = toCPointer<intptr_tVar>() ?: return null
        return interpretObjCPointerOrNull(pointer.rawValue)
    } catch (_: Exception) {
        println("CANNOT LOCATE THIS PTR")
        return null
    }
}

internal fun NSView.addDebugBorder(color: NSColor = NSColor.whiteColor, width: Double = 2.0) {
    this.wantsLayer = true
    this.layer?.borderWidth = width
    this.layer?.borderColor = color.CGColor
}

internal fun dispatchOnMain(block: () -> Unit) {
    if (NSThread.isMainThread) block()
    else dispatch_async(dispatch_get_main_queue(), block)
}
