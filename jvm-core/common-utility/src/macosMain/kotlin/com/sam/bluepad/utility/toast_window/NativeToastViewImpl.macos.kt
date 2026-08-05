package com.sam.bluepad.utility.toast_window

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.NSLogWriter
import co.touchlab.kermit.Severity
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.readValue
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
import platform.Foundation.NSClassFromString
import platform.Foundation.NSRect
import platform.Foundation.NSTimer
import platform.Foundation.NSZeroRect
import platform.QuartzCore.CATransform3DIdentity

@OptIn(BetaInteropApi::class)
actual class NativeToastViewImpl : INativeToastView {

    actual override fun createView(parentHandle: Long): Long {

        _logger.d { "CREATE CALLED ON MACOS WITH PARENT HANDLE:$parentHandle" }

        val parentView = parentHandle.toNSView() ?: run {
            _logger.e { "INVALID PARENT HANDLE" }
            return -1L
        }
        val parentWindow = parentView.window ?: run {
            _logger.e { "PARENT VIEW HAS NO WINDOW" }
            return 1L
        }

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
            appearance = platform.AppKit.NSAppearance.appearanceNamed(platform.AppKit.NSAppearanceNameVibrantDark)
            setReleasedWhenClosed(false)
        }

        val rootView = NSView(NSZeroRect.readValue()).apply {
            wantsLayer = false
        }

        val backdrop: NSView = if (NSClassFromString("NSGlassEffectView") != null) {
            _logger.d { "USING NSGlassEffectView" }
            NSGlassEffectView(NSZeroRect.readValue()).apply {
                cornerRadius = 12.0
                tintColor = null
            }
        } else {
            _logger.d { "USING NSVisualEffectView" }
            NSVisualEffectView(frame = NSZeroRect.readValue()).apply {
                blendingMode = NSVisualEffectBlendingMode.NSVisualEffectBlendingModeBehindWindow
                material = NSVisualEffectMaterialHUDWindow
                state = NSVisualEffectState.NSVisualEffectStateActive
                wantsLayer = true
                layer?.masksToBounds = true
                layer?.cornerRadius = 12.0
            }
        }

        val labelContainer = NSView(NSZeroRect.readValue()).apply {
            wantsLayer = false
        }

        val label = NSTextField(frame = NSZeroRect.readValue()).apply {
            bezeled = false
            bordered = false
            drawsBackground = false
            editable = false
            selectable = false
            alphaValue = 1.0
            alignment = NSTextAlignmentCenter
            lineBreakMode = NSLineBreakByTruncatingTail
            textColor = NSColor.whiteColor
            font = NSFont.systemFontOfSize(13.0)
        }

        labelContainer.addSubview(label)

        when (backdrop) {
            is NSGlassEffectView -> backdrop.contentView = labelContainer
            else -> backdrop.addSubview(labelContainer)
        }

        rootView.addSubview(backdrop)
        window.contentView = rootView

        parentWindow.addChildWindow(window, NSWindowAbove)
        window.orderOut(null)
        _logger.d { "WINDOW ORDERED OUT INITIALLY" }

        saveWindow(Keys.CURRENT_WINDOW, rootView, window)
        saveWindow(Keys.PARENT_WINDOW, rootView, parentWindow)

        val rawPtr = rootView.objcPtr()

        _logger.d { "BACKDROP CREATED : ${backdrop::class.simpleName}" }
        _logger.d { "RETURNING ROOT HANDLE : ${rawPtr.toLong()}" }

        return rawPtr.toLong()
    }

    actual override fun destroyView(viewHandle: Long) = dispatchOnMain {
        _logger.d { "DESTROY VIEW CALLED (handle=$viewHandle)" }

        val rootView = viewHandle.toNSView() ?: run {
            _logger.e { "FAILED TO CONVERT HANDLE TO NSView" }
            return@dispatchOnMain
        }

        val window = getWindow(Keys.CURRENT_WINDOW, rootView) ?: run {
            _logger.d { "NO ASSOCIATED WINDOW FOUND" }
            return@dispatchOnMain
        }

        val parent = getWindow(Keys.PARENT_WINDOW, rootView)

        try {
            hideTimer?.invalidate()
            hideTimer = null

            // Clear associated objects FIRST while rootView is active and valid
            saveWindow(Keys.CURRENT_WINDOW, rootView, null)
            saveWindow(Keys.PARENT_WINDOW, rootView, null)

            window.orderOut(null)
            parent?.removeChildWindow(window)
            window.close()
            _logger.d { "WINDOW CLOSED" }
        } catch (e: Exception) {
            _logger.e(e) { "ERROR WHILE CLOSING WINDOW" }
        } finally {
            _logger.d { "DESTROY VIEW COMPLETED" }
        }
    }

    actual override fun setBounds(viewHandle: Long, x: Int, y: Int, width: Int, height: Int) = dispatchOnMain {
        _logger.d { "SET BOUNDS CALLED : x=$x y=$y width=$width height=$height" }

        val rootView = viewHandle.toNSView() ?: run {
            _logger.e { "SET BOUNDS FAILED: rootView is null for handle $viewHandle" }
            return@dispatchOnMain
        }
        _logger.d { "SET BOUNDS: rootView resolved successfully: $rootView" }

        val window = getWindow(Keys.CURRENT_WINDOW, rootView) ?: run {
            _logger.e { "SET BOUNDS FAILED: window is null for rootView $rootView" }
            return@dispatchOnMain
        }
        _logger.d { "SET BOUNDS: window resolved successfully: $window" }

        val backdrop = rootView.getFirstSubView() ?: run {
            _logger.e { "SET BOUNDS FAILED: backdrop view is null for rootView $rootView" }
            return@dispatchOnMain
        }
        _logger.d { "SET BOUNDS: backdrop resolved successfully: $backdrop" }

        // 1. Position the borderless window on the screen
        val windowFrame = cValue<NSRect> {
            origin.x = x.toDouble()
            origin.y = y.toDouble()
            size.width = width.toDouble()
            size.height = height.toDouble()
        }
        window.setFrame(frameRect = windowFrame, display = true, animate = false)
        _logger.d { "SET BOUNDS: window frame updated to x=$x, y=$y, width=$width, height=$height" }

        val fillBounds = cValue<NSRect> {
            origin.x = -10.0
            origin.y = -10.0
            size.width = width.toDouble()
            size.height = height.toDouble()
        }

        rootView.setFrame(fillBounds)
        backdrop.setFrame(fillBounds)
        _logger.d { "SET BOUNDS: rootView and backdrop frames updated with fillBounds" }

        // 3. Resolve and size the label container relative to its parent
        val labelContainer = when (backdrop) {
            is NSGlassEffectView -> backdrop.contentView
            else -> backdrop.subviews.firstOrNull() as? NSView
        } ?: run {
            _logger.e { "SET BOUNDS FAILED: labelContainer is null for backdrop $backdrop" }
            return@dispatchOnMain
        }
        _logger.d { "SET BOUNDS: labelContainer resolved successfully: $labelContainer" }

        labelContainer.setFrame(fillBounds)

        // 4. Inset the text label inside the container
        val container = rootView.getContainerView() ?: run {
            _logger.e { "SET BOUNDS FAILED: container is null for rootView $rootView" }
            return@dispatchOnMain
        }
        _logger.d { "SET BOUNDS: container resolved successfully: $container" }

        val label = container.findNSTextField() ?: run {
            _logger.e { "SET BOUNDS FAILED: label (NSTextField) not found in container $container" }
            return@dispatchOnMain
        }
        _logger.d { "SET BOUNDS: label resolved successfully: $label" }

        val labelWidth = (width.toDouble() - (HORIZONTAL_PADDING * 2)).coerceAtLeast(0.0)
        val labelHeight = (height.toDouble() - (VERTICAL_PADDING * 2)).coerceAtLeast(0.0)

        val labelFrame = cValue<NSRect> {
            origin.x = HORIZONTAL_PADDING
            origin.y = VERTICAL_PADDING
            size.width = labelWidth
            size.height = labelHeight
        }

        label.setFrame(labelFrame)

        _logger.d { "SET BOUNDS COMPLETED SUCCESSFULLY" }

    }

    actual override fun setCornerRadius(viewHandle: Long, radius: Float) = dispatchOnMain {
        _logger.d { "SET CORNER RADIUS CALLED : radius=$radius" }

        val rootView = viewHandle.toNSView() ?: run {
            _logger.e { "FAILED TO CONVERT HANDLE TO NSView" }
            return@dispatchOnMain
        }

        val contentView = rootView.getFirstSubView() ?: run {
            _logger.e { "FAILED TO RESOLVE CONTENT VIEW FOR CORNER RADIUS" }
            return@dispatchOnMain
        }

        val radi = radius.toDouble()

        when (contentView) {
            is NSGlassEffectView -> contentView.cornerRadius = radi
            else -> {
                contentView.wantsLayer = true
                contentView.layer?.apply {
                    cornerRadius = radi
                    masksToBounds = true
                }
            }
        }
        _logger.d { "CORNER RADIUS UPDATED TO $radi" }
    }


    actual override fun setBackgroundColor(viewHandle: Long, color: Int) = dispatchOnMain {
        _logger.d { "SET BACKGROUND COLOR CALLED : 0x${color.toUInt().toString(16)}" }

        val rootView = viewHandle.toNSView() ?: run {
            _logger.e { "FAILED TO CONVERT HANDLE TO NSView" }
            return@dispatchOnMain
        }

        val contentView = rootView.getFirstSubView() ?: run {
            _logger.e { "WINDOW HAS NO CONTENT VIEW SET COLOR" }
            return@dispatchOnMain
        }

        val a = ((color shr 24) and 0xFF) / 255.0
        val r = ((color shr 16) and 0xFF) / 255.0
        val g = ((color shr 8) and 0xFF) / 255.0
        val b = (color and 0xFF) / 255.0

        _logger.d { "PARSED ARGB = ($a, $r, $g, $b)" }

        val nsColor = NSColor.colorWithSRGBRed(r, g, b, a)

        when (contentView) {
            is NSGlassEffectView -> {
                contentView.tintColor = if (a > 0.0) nsColor else null
                _logger.d { "NSGlassEffectView TINT COLOR UPDATED" }
            }

            else -> {
                contentView.wantsLayer = true
                contentView.layer?.backgroundColor = if (a > 0.0) nsColor.CGColor else null
                _logger.d { if (a > 0.0) "BACKGROUND COLOR SET" else "BACKGROUND COLOR CLEARED" }
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
        _logger.d { "SHOW CALLED : text=$text fadeIn=${fadeInMs}ms hold=${holdMs}ms fadeOut=${fadeOutMs}ms" }

        val rootView = viewHandle.toNSView() ?: return@dispatchOnMain
        val window = getWindow(Keys.CURRENT_WINDOW, rootView) ?: return@dispatchOnMain

        val backdrop = rootView.getFirstSubView() ?: return@dispatchOnMain
        val container = rootView.getContainerView() ?: return@dispatchOnMain
        val label = container.findNSTextField() ?: return@dispatchOnMain

        hideTimer?.invalidate()
        val generation = ++animationGeneration

        hideTimer?.invalidate()
        hideTimer = null

        // Interrupt any existing toast
        if (window.alphaValue > 0.0) {
            _logger.d { "INTERRUPTING ACTIVE TOAST" }

            backdrop.removeToastAnimations()

            window.orderOut(null)
            window.alphaValue = 0.0
            container.alphaValue = 0.0
        }

        label.stringValue = text

        label.needsDisplay = true
        label.displayIfNeeded()

        container.needsLayout = true
        container.layoutSubtreeIfNeeded()
        container.needsDisplay = true
        container.displayIfNeeded()

        backdrop.removeToastAnimations()

        window.alphaValue = 0.0
        container.alphaValue = 1.0

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

                // single invocation for the timer
                hideTimer = NSTimer.scheduledTimerWithTimeInterval(holdMs / 1000.0, false) {

                    if (generation != animationGeneration) {
                        _logger.d { "FADE-OUT CANCELLED BY NEWER TOAST" }
                        return@scheduledTimerWithTimeInterval
                    }

                    _logger.d { "STARTING FADE-OUT" }
                    backdrop.playPopOut(fadeOutMs)

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

                            backdrop.removeToastAnimations()
                            window.alphaValue = 0.0
                            container.alphaValue = 1.0
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
                override val minSeverity: Severity = Severity.Debug
                override val logWriterList: List<LogWriter> = listOf(NSLogWriter())
            },
            tag = "NATIVE_WINDOW",
        )

        private var hideTimer: NSTimer? = null
        private var animationGeneration = 0L

        private const val HORIZONTAL_PADDING = 8.0
        private const val VERTICAL_PADDING = 6.0
    }

}
