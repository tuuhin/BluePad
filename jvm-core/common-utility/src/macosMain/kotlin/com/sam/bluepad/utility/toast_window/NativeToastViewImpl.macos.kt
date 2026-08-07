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
import platform.AppKit.NSColor
import platform.AppKit.NSFont
import platform.AppKit.NSGlassEffectView
import platform.AppKit.NSGlassEffectViewStyle
import platform.AppKit.NSLineBreakByTruncatingTail
import platform.AppKit.NSTextAlignmentCenter
import platform.AppKit.NSTextField
import platform.AppKit.NSView
import platform.AppKit.NSVisualEffectBlendingMode
import platform.AppKit.NSVisualEffectMaterialWindowBackground
import platform.AppKit.NSVisualEffectState
import platform.AppKit.NSVisualEffectView
import platform.AppKit.cell
import platform.Foundation.NSClassFromString
import platform.Foundation.NSRect
import platform.Foundation.NSZeroRect
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.objc.objc_removeAssociatedObjects

/**
 * macOS implementation of a native toast view using AppKit.
 *
 * This class provides a lightweight, platform-native toast component backed by
 * either [NSGlassEffectView] (preferred) or [NSVisualEffectView] (fallback).
 * It is designed to be used with the Tao/Nucleus native view system where
 * lifecycle and layout are controlled externally.
 *
 * ---
 * #### View Hierarchy
 *
 * ```mermaid
 * NSGlassEffectView / NSVisualEffectView (container)
 * └── NSTextField (label)
 * ```
 * ---
 * #### Visibility Model
 *
 * - The view is **hidden by default** (`hidden = true`, `alpha = 0`)
 * - [show] makes it visible, animates it in, and schedules dismissal
 * - After fade-out, the view is hidden again
 * - This avoids initial rendering flashes before first use.
 *
 * ---
 * #### Layout Strategy
 *
 * - Manual frame-based layout (no AutoLayout)
 * - Padding is applied inside the container for label positioning
 * - NSTextField requires an **explicit height** (no intrinsic sizing)
 *
 * Note: NSTextField does not vertically center text by default; small
 * manual offsets may be applied if needed.
 */
@OptIn(BetaInteropApi::class)
actual class NativeToastViewImpl : INativeToastView {

    actual override fun createView(parentHandle: Long): Long {
        _logger.d { "CREATE CALLED ON MACOS WITH PARENT HANDLE:$parentHandle" }

        val labelView = NSTextField(frame = NSZeroRect.readValue()).apply {
            bezeled = false
            bordered = false
            drawsBackground = false
            editable = false
            selectable = false

            alignment = NSTextAlignmentCenter
            lineBreakMode = NSLineBreakByTruncatingTail
            maximumNumberOfLines = 1

            textColor = NSColor.labelColor
            font = NSFont.systemFontOfSize(13.0)

            identifier = TOAST_LABEL_IDENTIFIER

            cell?.usesSingleLineMode = true
            cell?.wraps = false
            cell?.scrollable = true
            cell?.lineBreakMode = NSLineBreakByTruncatingTail
            cell?.alignment = NSTextAlignmentCenter
        }

        val containerView: NSView = if (NSClassFromString("NSGlassEffectView") != null) {
            _logger.d { "USING NSGlassEffectView" }
            NSGlassEffectView(NSZeroRect.readValue()).apply {
                cornerRadius = 12.0
                // can update the style based to regular
                style = NSGlassEffectViewStyle.NSGlassEffectViewStyleRegular
                addSubview(labelView)
                identifier = TOAST_BACKGROUND_IDENTIFIER
            }
        } else {
            _logger.d { "USING NSVisualEffectView" }
            NSVisualEffectView(frame = NSZeroRect.readValue()).apply {
                blendingMode = NSVisualEffectBlendingMode.NSVisualEffectBlendingModeBehindWindow
                material = NSVisualEffectMaterialWindowBackground
                state = NSVisualEffectState.NSVisualEffectStateActive

                wantsLayer = true
                layer?.masksToBounds = true
                layer?.cornerRadius = 12.0

                addSubview(labelView)
                identifier = TOAST_BACKGROUND_IDENTIFIER
            }
        }

        containerView.hidden = true
        containerView.alphaValue = 0.0

        _logger.d { "WINDOW ORDERED OUT INITIALLY" }
        saveViewRefs(containerView, labelView)
        val rawPtr = containerView.objcPtr()
        return rawPtr.toLong()
    }

    actual override fun destroyView(viewHandle: Long) = dispatchOnMain {
        _logger.d { "DESTROY VIEW CALLED (handle=$viewHandle)" }

        val rootView = viewHandle.toNSView() ?: return@dispatchOnMain
        _logger.d { "DESTROYING THE ROOT VIEW" }

        val label = rootView.getLabel()

        try {
            rootView.layer?.removeAllAnimations()
            label?.layer?.removeAllAnimations()
            rootView.removeFromSuperview()
            _logger.d { "VIEW HIERARCHY REMOVED" }
        } catch (e: Exception) {
            _logger.e(e) { "ERROR WHILE CLOSING WINDOW" }
        } finally {
            // Clear associated objects FIRST while rootView is active and valid
            objc_removeAssociatedObjects(rootView)
            _logger.d { "DESTROY VIEW COMPLETED AND REMOVED ASSOCIATED OBJECTS" }
        }
    }

    actual override fun setBounds(viewHandle: Long, x: Int, y: Int, width: Int, height: Int) = dispatchOnMain {
        _logger.d { "SET BOUNDS CALLED : x=$x y=$y width=$width height=$height" }

        val rootView = viewHandle.toNSView() ?: return@dispatchOnMain
        val label = rootView.getLabel() ?: return@dispatchOnMain

        val rootFrame = cValue<NSRect> {
            origin.x = x.toDouble()
            origin.y = y.toDouble()
            size.width = width.toDouble()
            size.height = height.toDouble()
        }
        rootView.setFrame(rootFrame)

        val padding = 6.0

        val labelFrame = cValue<NSRect> {
            origin.x = padding
            origin.y = padding
            size.width = width / 2.0 - (padding * 2)
            size.height = height / 2.0 - (padding * 2)
        }
        label.setFrame(labelFrame)
        _logger.d { "SET BOUNDS APPLIED SUCCESSFULLY" }
    }

    actual override fun setCornerRadius(viewHandle: Long, radius: Float) = dispatchOnMain {
        _logger.d { "SET CORNER RADIUS CALLED : radius=$radius" }

        val rootView = viewHandle.toNSView() ?: return@dispatchOnMain

        when (rootView) {
            is NSGlassEffectView -> rootView.cornerRadius = radius.toDouble()
            else -> {
                rootView.wantsLayer = true
                rootView.layer?.apply {
                    cornerRadius = radius.toDouble()
                    masksToBounds = true
                }
            }
        }
        _logger.d { "CORNER RADIUS UPDATED TO $radius" }
    }


    actual override fun setBackgroundColor(viewHandle: Long, color: Int) = dispatchOnMain {
        _logger.d { "SET BACKGROUND COLOR CALLED : 0x${color.toUInt().toString(16)}" }

        val rootView = viewHandle.toNSView() ?: return@dispatchOnMain

        val a = ((color shr 24) and 0xFF) / 255.0
        val r = ((color shr 16) and 0xFF) / 255.0
        val g = ((color shr 8) and 0xFF) / 255.0
        val b = (color and 0xFF) / 255.0

        val nsColor = NSColor.colorWithSRGBRed(r, g, b, a)

        when (rootView) {
            is NSGlassEffectView -> _logger.d { "NO COLOR FOR GLASS EFFECT VIEW" }
            else -> {
                rootView.wantsLayer = true
                rootView.layer?.backgroundColor = if (a > 0.0) nsColor.CGColor else null
            }
        }
        _logger.d { "BACKGROUND COLOR APPLIED" }
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
        val label = rootView.getLabel() ?: return@dispatchOnMain

        val generation = ++animationGeneration

        label.stringValue = text

        rootView.removeToastAnimations()
        rootView.layer?.removeAllAnimations()

        rootView.hidden = false
        rootView.alphaValue = 0.0

        _logger.d { "WINDOW ORDERED TO FRONT" }
        rootView.playPopIn(fadeInMs)

        NSAnimationContext.runAnimationGroup(
            changes = { context ->
                context?.duration = fadeInMs / 1000.0
                context?.allowsImplicitAnimation = true
                rootView.animator().alphaValue = 1.0
            },
            completionHandler = {
                if (generation != animationGeneration) {
                    _logger.d { "FADE-IN INVALIDATED" }
                    return@runAnimationGroup
                }

                val delayNs = holdMs * 1_000_000L

                dispatch_after(
                    dispatch_time(DISPATCH_TIME_NOW, delayNs),
                    dispatch_get_main_queue(),
                ) {
                    if (generation != animationGeneration) {
                        _logger.d { "FADE-OUT CANCELLED BY NEWER TOAST" }
                        return@dispatch_after
                    }

                    rootView.playPopOut(fadeOutMs)
                    _logger.d { "STARTING FADE-OUT" }
                    NSAnimationContext.runAnimationGroup(
                        changes = { context ->
                            context?.duration = fadeOutMs / 1000.0
                            context?.allowsImplicitAnimation = true
                            rootView.animator().alphaValue = 0.0
                        },
                        completionHandler = {
                            if (generation != animationGeneration) {
                                _logger.d { "COMPLETION INVALIDATED" }
                                return@runAnimationGroup
                            }
                            _logger.d { "TOAST HIDDEN" }
                            rootView.hidden = true
                            rootView.removeToastAnimations()
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

        private var animationGeneration = 0L

        private const val TOAST_BACKGROUND_IDENTIFIER = "toast_background_indentifier"
        private const val TOAST_LABEL_IDENTIFIER = "toast_label_indentifier"
    }

}
