package com.sam.bluepad.utility.toast_window

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.NSLogWriter
import co.touchlab.kermit.Severity
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.cValue
import kotlinx.cinterop.interpretObjCPointerOrNull
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.objc_release
import kotlinx.cinterop.objc_retain
import kotlinx.cinterop.readValue
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.usePinned
import platform.AppKit.NSAnimationContext
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSColor
import platform.AppKit.NSFloatingWindowLevel
import platform.AppKit.NSFont
import platform.AppKit.NSGlassEffectView
import platform.AppKit.NSLineBreakByTruncatingTail
import platform.AppKit.NSTextAlignmentCenter
import platform.AppKit.NSTextField
import platform.AppKit.NSView
import platform.AppKit.NSVisualEffectBlendingMode
import platform.AppKit.NSVisualEffectMaterialHUDWindow
import platform.AppKit.NSVisualEffectState
import platform.AppKit.NSVisualEffectView
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowAbove
import platform.AppKit.NSWindowCollectionBehaviorTransient
import platform.AppKit.NSWindowStyleMaskBorderless
import platform.AppKit.translatesAutoresizingMaskIntoConstraints
import platform.Foundation.NSClassFromString
import platform.Foundation.NSRect
import platform.Foundation.NSTimer
import platform.Foundation.NSZeroRect
import platform.Foundation.className
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_getAssociatedObject
import platform.objc.objc_setAssociatedObject
import platform.posix.intptr_tVar

@OptIn(BetaInteropApi::class)
actual class NativeToastViewImpl : INativeToastView {

    actual override fun createView(parentHandle: Long): Long {

        _logger.d { "CREATE CALLED ON MACOS WITH PARENT HANDLE:$parentHandle" }
        val parentView = parentHandle.toNSView() ?: return -1L
        val parentWindow = parentView.window ?: return 1L

        _logger.d { "BACKEND WINDOW ACQUIRED: $parentWindow" }

        val window = NSWindow(
            contentRect = NSZeroRect.readValue(),
            styleMask = NSWindowStyleMaskBorderless,
            backing = NSBackingStoreBuffered,
            defer = false,
        ).apply {
            opaque = false
            backgroundColor = NSColor.clearColor
            hasShadow = true
            level = NSFloatingWindowLevel
            alphaValue = 0.0
            ignoresMouseEvents = true
            collectionBehavior = NSWindowCollectionBehaviorTransient
            excludedFromWindowsMenu = true
        }

        _logger.d { "WINDOW CREATED : $window" }
        val rootView = NSView(NSZeroRect.readValue()).apply {
            wantsLayer = false
            translatesAutoresizingMaskIntoConstraints = false
        }
        val backdrop: NSView = if (NSClassFromString("NSGlassEffectView") != null) {
            _logger.d { "USING NSGlassEffectView" }
            NSGlassEffectView(NSZeroRect.readValue()).apply {
                cornerRadius = 12.0
                tintColor = null
                translatesAutoresizingMaskIntoConstraints = false
            }
        } else {
            _logger.d { "USING NSVisualEffectView" }
            NSVisualEffectView(frame = NSZeroRect.readValue()).apply {
                blendingMode =
                    NSVisualEffectBlendingMode.NSVisualEffectBlendingModeBehindWindow
                material = NSVisualEffectMaterialHUDWindow
                state = NSVisualEffectState.NSVisualEffectStateActive
                wantsLayer = true
                layer?.masksToBounds = true
                layer?.cornerRadius = 12.0
                translatesAutoresizingMaskIntoConstraints = false
            }
        }

        val container = NSView(NSZeroRect.readValue())

        val label = NSTextField(frame = NSZeroRect.readValue()).apply {
            bezeled = false
            drawsBackground = false
            editable = false
            selectable = false
            alignment = NSTextAlignmentCenter
            lineBreakMode = NSLineBreakByTruncatingTail
            textColor = NSColor.labelColor
            font = NSFont.systemFontOfSize(13.0)
        }

        if (backdrop is NSGlassEffectView) backdrop.contentView = label
        else backdrop.addSubview(label)

        rootView.addSubview(backdrop)
        window.contentView = rootView

        parentWindow.addChildWindow(window, NSWindowAbove)

        CURRENT_WINDOW_KEY.usePinned { pinned ->
            objc_setAssociatedObject(
                rootView,
                pinned.addressOf(0),
                window,
                OBJC_ASSOCIATION_RETAIN_NONATOMIC,
            )
        }

        val rawPtr = rootView.objcPtr()
        objc_retain(rawPtr)

        _logger.d { "BACKDROP CREATED : ${backdrop::class.simpleName}" }
        _logger.d { "WINDOW CONTENT VIEW : ${window.contentView}" }
        _logger.d { "RETURNING ROOT HANDLE : ${rawPtr.toLong()}" }

        return rawPtr.toLong()
    }

    actual override fun destroyView(viewHandle: Long) {
        _logger.d { "DESTROY VIEW CALLED (handle=$viewHandle)" }

        val rootView = viewHandle.toNSView() ?: run {
            _logger.e { "FAILED TO CONVERT HANDLE TO NSView" }
            return
        }

        _logger.d { "NS VIEW RESOLVED : $rootView" }

        val window = CURRENT_WINDOW_KEY.usePinned { pinned ->
            objc_getAssociatedObject(rootView, pinned.addressOf(0)) as? NSWindow
        }

        try {
            if (window == null) {
                _logger.w { "NO ASSOCIATED WINDOW FOUND" }
                return
            }
            window.orderOut(null)
            window.contentView?.removeFromSuperview()
            window.contentView = null
            window.close()
            _logger.d { "WINDOW CLOSED" }

            hideTimer?.invalidate()
            hideTimer = null
            animationGeneration = 0
        } finally {
            rootView.removeFromSuperview()
            CURRENT_WINDOW_KEY.usePinned { pinned ->
                objc_setAssociatedObject(
                    rootView,
                    pinned.addressOf(0),
                    null,
                    OBJC_ASSOCIATION_RETAIN_NONATOMIC,
                )
            }
            objc_release(rootView.objcPtr())
            _logger.d { "DESTROY VIEW COMPLETED" }
        }
    }

    actual override fun setBounds(viewHandle: Long, x: Int, y: Int, width: Int, height: Int) {
        _logger.d { "SET BOUNDS CALLED : x=$x y=$y width=$width height=$height" }

        val rootView = viewHandle.toNSView() ?: run {
            _logger.e { "FAILED TO CONVERT HANDLE TO NSView" }
            return
        }

        val window = CURRENT_WINDOW_KEY.usePinned { pinned ->
            objc_getAssociatedObject(rootView, pinned.addressOf(0)) as? NSWindow
        } ?: run {
            _logger.e { "FAILED TO RESOLVE ASSOCIATED WINDOW" }
            return
        }

        val contentView = rootView.getBackdrop() ?: run {
            _logger.e { "FAILED TO RESOLVE BACKDROP VIEW" }
            return
        }

        val screenFrame = cValue<NSRect> {
            origin.x = x.toDouble()
            origin.y = y.toDouble()
            size.width = width.toDouble()
            size.height = height.toDouble()
        }

        _logger.d { "SET WINDOW" }
        window.setFrame(frameRect = screenFrame, display = false, animate = false)

        val label = when (contentView) {
            is NSGlassEffectView -> contentView.contentView as? NSTextField
            else -> contentView.subviews.firstOrNull() as? NSTextField
        } ?: return


        val font = label.font ?: NSFont.systemFontOfSize(13.0)
        val labelHeight = font.ascender - font.descender + font.leading

        val labelFrame = cValue<NSRect> {
            origin.x = 8.0
            origin.y = (height - labelHeight) / 2.0
            size.width = (width - 16).toDouble()
            size.height = labelHeight
        }

        label.setFrame(labelFrame)
        _logger.d { "SET BOUNDS COMPLETED" }
    }

    actual override fun setCornerRadius(viewHandle: Long, radius: Float) {
        _logger.d { "SET CORNER RADIUS CALLED : radius=$radius" }

        val rootView = viewHandle.toNSView() ?: run {
            _logger.e { "FAILED TO CONVERT HANDLE TO NSView" }
            return
        }

        val contentView = rootView.getBackdrop() ?: return
        val classNAme = NSClassFromString(contentView.className)
        _logger.d { "UPDATED VIEW:$classNAme CORNER RADIUS" }

        when (contentView) {
            is NSGlassEffectView -> contentView.cornerRadius = radius.toDouble()
            is NSVisualEffectView -> {
                contentView.wantsLayer = true
                contentView.layer?.masksToBounds = true
                contentView.layer?.cornerRadius = radius.toDouble()
            }

            else -> {
                contentView.wantsLayer = true
                contentView.layer?.masksToBounds = true
                contentView.layer?.cornerRadius = radius.toDouble()
            }
        }
    }

    actual override fun setBackgroundColor(viewHandle: Long, color: Int) {
        _logger.d { "SET BACKGROUND COLOR CALLED : 0x${color.toUInt().toString(16)}" }

        val rootView = viewHandle.toNSView() ?: run {
            _logger.e { "FAILED TO CONVERT HANDLE TO NSView" }
            return
        }

        val window = CURRENT_WINDOW_KEY.usePinned { pinned ->
            objc_getAssociatedObject(rootView, pinned.addressOf(0)) as? NSWindow
        } ?: run {
            _logger.e { "FAILED TO RESOLVE ASSOCIATED WINDOW" }
            return
        }

        val contentView = rootView.getBackdrop() ?: run {
            _logger.e { "WINDOW HAS NO CONTENT VIEW SET COLOR" }
            return
        }

        val a = ((color shr 24) and 0xff) / 255.0
        val r = ((color shr 16) and 0xff) / 255.0
        val g = ((color shr 8) and 0xff) / 255.0
        val b = (color and 0xff) / 255.0

        _logger.d { "ARGB = ($a, $r, $g, $b)" }

        val nsColor = NSColor.colorWithSRGBRed(r, g, b, a)

        when (contentView) {
            is NSGlassEffectView -> _logger.w { "NSGlassEffectView does not support custom layer background colors. Ignoring request." }
            else -> {
                contentView.wantsLayer = true
                contentView.layer?.backgroundColor = if (a > 0.0) nsColor.CGColor else null
                _logger.d { "BACKGROUND COLOR CLEARED" }
            }
        }
    }

    actual override fun show(
        viewHandle: Long,
        text: String,
        fadeInMs: Int,
        holdMs: Int,
        fadeOutMs: Int
    ) = dispatchOnMain {

        _logger.d {
            "SHOW CALLED : text=$text fadeIn=${fadeInMs}ms hold=${holdMs}ms fadeOut=${fadeOutMs}ms"
        }

        val rootView = viewHandle.toNSView() ?: run {
            _logger.e { "FAILED TO RESOLVE ROOT VIEW FROM HANDLE" }
            return@dispatchOnMain
        }

        val window = CURRENT_WINDOW_KEY.usePinned { pinned ->
            objc_getAssociatedObject(rootView, pinned.addressOf(0)) as? NSWindow
        } ?: run {
            _logger.e { "FAILED TO RESOLVE ASSOCIATED WINDOW" }
            return@dispatchOnMain
        }

        val backdrop = rootView.getBackdrop() ?: run {
            _logger.e { "FAILED TO RESOLVE BACKDROP VIEW" }
            return@dispatchOnMain
        }

        val label = when (backdrop) {
            is NSGlassEffectView -> backdrop.contentView as? NSTextField
            else -> backdrop.subviews.firstOrNull() as? NSTextField
        } ?: run {
            _logger.e { "FAILED TO FIND TEXT LABEL" }
            return@dispatchOnMain
        }

        label.stringValue = text
        label.needsDisplay = true
        label.displayIfNeeded()

        backdrop.needsLayout = true
        backdrop.layoutSubtreeIfNeeded()
        backdrop.needsDisplay = true
        backdrop.displayIfNeeded()

        _logger.d { "TEXT UPDATED : '$text'" }

        animationGeneration++
        val generation = animationGeneration

        hideTimer?.invalidate()
        hideTimer = null

        window.alphaValue = 0.0
        window.orderFrontRegardless()

        _logger.d { "WINDOW ORDERED TO FRONT" }

        backdrop.playPopIn(fadeInMs)

        NSAnimationContext.runAnimationGroup(
            changes = { context ->
                context?.duration = fadeInMs / 1000.0
                context?.allowsImplicitAnimation = true

                window.animator().alphaValue = 1.0
            },
            completionHandler = {

                if (generation != animationGeneration) {
                    _logger.d { "FADE-IN INVALIDATED BY NEWER TOAST" }
                    return@runAnimationGroup
                }

                _logger.d { "FADE-IN COMPLETED" }

                hideTimer = NSTimer.scheduledTimerWithTimeInterval(
                    holdMs / 1000.0,
                    false,
                ) {

                    if (generation != animationGeneration) {
                        _logger.d { "FADE-OUT CANCELLED BY NEWER TOAST" }
                        return@scheduledTimerWithTimeInterval
                    }

                    _logger.d { "STARTING FADE-OUT" }

                    NSAnimationContext.runAnimationGroup(
                        changes = { context ->
                            context?.duration = fadeOutMs / 1000.0
                            context?.allowsImplicitAnimation = true

                            window.animator().alphaValue = 0.0
                        },
                        completionHandler = {

                            if (generation != animationGeneration) {
                                _logger.d { "COMPLETION INVALIDATED" }
                                return@runAnimationGroup
                            }

                            window.alphaValue = 0.0
                            window.orderOut(null)

                            _logger.d { "WINDOW HIDDEN" }
                        },
                    )
                }
            },
        )
    }


    companion object {

        private val _logger = Logger(
            config = object : LoggerConfig {
                override val minSeverity: Severity
                    get() = Severity.Debug

                override val logWriterList: List<LogWriter>
                    get() = listOf(NSLogWriter())
            },
            tag = "NATIVE_WINDOW",
        )

        private fun NSView.getBackdrop(): NSView? {
            return subviews.firstOrNull() as? NSView
        }

        private fun Long.toNSView(): NSView? {
            if (this == 0L || this == -1L) return null
            val pointer = toCPointer<intptr_tVar>() ?: return null

            return interpretObjCPointerOrNull<NSView>(pointer.rawValue)
        }

        private fun dispatchOnMain(block: () -> Unit) {
            dispatch_async(dispatch_get_main_queue(), block)
        }

        private var hideTimer: NSTimer? = null
        private var animationGeneration = 0L

        private val CURRENT_WINDOW_KEY = byteArrayOf(0)
    }

}
