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
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_getAssociatedObject
import platform.objc.objc_setAssociatedObject
import platform.posix.intptr_tVar

internal object ViewKeys {
    val BACKGROUND = nativeHeap.alloc<ByteVar>()
    val CONTAINER = nativeHeap.alloc<ByteVar>()
    val LABEL = nativeHeap.alloc<ByteVar>()
}

internal fun saveViewRefs(
    root: NSView,
    background: NSView,
    container: NSView,
    label: NSTextField
) {
    objc_setAssociatedObject(root, ViewKeys.BACKGROUND.ptr, background, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    objc_setAssociatedObject(root, ViewKeys.CONTAINER.ptr, container, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    objc_setAssociatedObject(root, ViewKeys.LABEL.ptr, label, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
}

internal fun NSView.getBackground(): NSView? =
    objc_getAssociatedObject(this, ViewKeys.BACKGROUND.ptr) as? NSView

internal fun NSView.getContainer(): NSView? =
    objc_getAssociatedObject(this, ViewKeys.CONTAINER.ptr) as? NSView

internal fun NSView.getLabel(): NSTextField? =
    objc_getAssociatedObject(this, ViewKeys.LABEL.ptr) as? NSTextField

internal fun NSView.getFirstChild(): NSView? = subviews.firstOrNull() as? NSView

internal fun NSView.contentViewOrFirstChild(): NSView? = when (this) {
    is NSGlassEffectView -> contentView
    else -> subviews.firstOrNull() as? NSView
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
