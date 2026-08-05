package com.sam.bluepad.utility.toast_window

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretObjCPointerOrNull
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCPointer
import platform.AppKit.NSColor
import platform.AppKit.NSGlassEffectView
import platform.AppKit.NSTextField
import platform.AppKit.NSView
import platform.AppKit.NSWindow
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_getAssociatedObject
import platform.objc.objc_setAssociatedObject
import platform.posix.intptr_tVar

internal object Keys {
    val CURRENT_WINDOW = nativeHeap.alloc<ByteVar>()
    val PARENT_WINDOW = nativeHeap.alloc<ByteVar>()
}

internal fun saveWindow(key: ByteVar, viewKey: NSView, window: NSWindow?) =
    objc_setAssociatedObject(viewKey, key.ptr, window, OBJC_ASSOCIATION_RETAIN_NONATOMIC)


internal fun getWindow(key: ByteVar, viewKey: NSView): NSWindow? =
    objc_getAssociatedObject(viewKey, key.ptr) as NSWindow?


internal fun NSView.getFirstSubView(): NSView? {
    return subviews.firstOrNull() as? NSView
}

/**
 * Returns the content view if the view is a [NSGlassEffectView] else the first subview
 */
internal fun NSView.getContainerView(): NSView? {
    return when (this) {
        is NSGlassEffectView -> contentView
        else -> subviews.firstOrNull() as? NSView
    }
}

internal fun NSView.findNSTextField(): NSTextField? {
    if (this is NSTextField) return this
    for (subview in subviews) {
        val nsView = subview as? NSView ?: continue
        if (nsView is NSTextField) return nsView
        val found = nsView.findNSTextField()
        if (found != null) return found
    }
    return null
}

internal fun Long.toNSView(): NSView? {
    if (this == 0L || this == -1L) return null
    val pointer = toCPointer<intptr_tVar>() ?: return null

    return interpretObjCPointerOrNull<NSView>(pointer.rawValue)
}

internal fun NSView.addDebugBorder(color: NSColor = NSColor.whiteColor, width: Double = 2.0) {
    this.wantsLayer = true
    this.layer?.borderWidth = width
    this.layer?.borderColor = color.CGColor
}

internal fun dispatchOnMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue(), block)
}
