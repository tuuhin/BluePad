package com.sam.bluepad.share_sheet

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.cValue
import kotlinx.cinterop.interpretObjCPointerOrNull
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.AppKit.NSSharingService
import platform.AppKit.NSSharingServiceDelegateProtocol
import platform.AppKit.NSSharingServicePicker
import platform.AppKit.NSSharingServicePickerDelegateProtocol
import platform.AppKit.NSView
import platform.Foundation.NSRect
import platform.Foundation.NSRectEdgeMinY
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject
import platform.posix.intptr_tVar

actual class NativeShareSheetImpl : INativeShareSheet {

    actual override fun shareTitleAndContent(windowHandle: Long, title: String, content: String) {

        val view = windowHandle.toNSView() ?: return

        val contentList = listOf(title, content)
        val delegate = SharingDelegate("Sharing data")
        val picker = NSSharingServicePicker(items = contentList).apply {
            this.delegate = delegate
        }

        DELEGATE_STORE_KEY.usePinned { pinned ->
            objc_setAssociatedObject(picker, pinned.addressOf(0), delegate, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }

        val bounds = view.bounds
        val positioningRect = cValue<NSRect> {
            origin.x = bounds.useContents { size.width / 2.0 - 50.0 }
            origin.y = bounds.useContents { size.height / 2.0 - 25.0 }
            size.width = 100.0
            size.height = 50.0
        }
        picker.showRelativeToRect(
            rect = positioningRect,
            ofView = view,
            preferredEdge = NSRectEdgeMinY,
        )
    }

    actual override fun shareText(windowHandle: Long, text: String) {

        val view = windowHandle.toNSView() ?: return
        val contentList = listOf(text)
        val delegate = SharingDelegate("Sharing data")
        val picker = NSSharingServicePicker(items = contentList).apply {
            this.delegate = delegate
        }

        DELEGATE_STORE_KEY.usePinned { pinned ->
            objc_setAssociatedObject(picker, pinned.addressOf(0), delegate, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }

        val bounds = view.bounds
        // TODO: TRY TO FIX THE WINDOW LAYOUT POSITION LATER
        val positioningRect = cValue<NSRect> {
            origin.x = bounds.useContents { size.width / 2.0 - 50.0 }
            origin.y = bounds.useContents { size.height / 2.0 - 25.0 }
            size.width = 100.0
            size.height = 50.0
        }
        picker.showRelativeToRect(
            rect = positioningRect,
            ofView = view,
            preferredEdge = NSRectEdgeMinY,
        )
    }

    companion object {
        private fun Long.toNSView(): NSView? {
            if (this == 0L || this == -1L) return null
            val pointer = toCPointer<intptr_tVar>() ?: return null

            return interpretObjCPointerOrNull<NSView>(pointer.rawValue)
        }

        private val DELEGATE_STORE_KEY = byteArrayOf(0)

        private class SharingDelegate(val title: String) : NSObject(),
            NSSharingServicePickerDelegateProtocol, NSSharingServiceDelegateProtocol {

            override fun sharingServicePicker(
                sharingServicePicker: NSSharingServicePicker,
                delegateForSharingService: NSSharingService
            ): NSSharingServiceDelegateProtocol = this
        }
    }
}
