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
import kotlinx.cinterop.useContents
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
import platform.Foundation.NSClassFromString
import platform.Foundation.NSRect
import platform.Foundation.NSZeroRect
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.objc.objc_removeAssociatedObjects

/**
 * Creates a floating borderless toast window using AppKit's glass effect
 * View hierarchy:
 *
 * ```mermaid
 *      RootView (NSView)
 *     └── GlassEffectView (NSGlassEffectView / NSVisualEffectView)
 *         └── LabelContainer (NSView)
 *             └── Label (NSTextField)
 * ```
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

            textColor = NSColor.labelColor
            font = NSFont.systemFontOfSize(13.0)

            identifier = TOAST_LABEL_IDENTIFIER
        }

        val labelContainerView = NSView(NSZeroRect.readValue()).apply {
            wantsLayer = true
            layer?.masksToBounds = false
            addSubview(labelView)
            identifier = TOAST_LABEL_CONTAINER_IDENTIFIER
        }

        val containerView: NSView = if (NSClassFromString("NSGlassEffectView") != null) {
            _logger.d { "USING NSGlassEffectView" }
            NSGlassEffectView(NSZeroRect.readValue()).apply {
                cornerRadius = 12.0
                // can update the style based to regular
                style = NSGlassEffectViewStyle.NSGlassEffectViewStyleClear
                contentView = labelContainerView
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

                addSubview(labelContainerView)
                identifier = TOAST_BACKGROUND_IDENTIFIER
            }
        }

        val rootView = NSView(frame = NSZeroRect.readValue()).apply {
            wantsLayer = true
            identifier = TOAST_ROOT_IDENTIFIER

            addSubview(containerView)
        }
        _logger.d { "WINDOW ORDERED OUT INITIALLY" }
        saveViewRefs(rootView, containerView, labelContainerView, labelView)
        val rawPtr = rootView.objcPtr()
        return rawPtr.toLong()
    }

    actual override fun destroyView(viewHandle: Long) = dispatchOnMain {
        _logger.d { "DESTROY VIEW CALLED (handle=$viewHandle)" }

        val rootView = viewHandle.toNSView() ?: return@dispatchOnMain
        _logger.d { "DESTROYING THE ROOT VIEW" }

        val background = rootView.getBackground()
        val container = rootView.getContainer()
        val label = rootView.getLabel()

        try {
            background?.layer?.removeAllAnimations()
            container?.layer?.removeAllAnimations()

            container?.alphaValue = 0.0

            label?.removeFromSuperview()
            container?.removeFromSuperview()
            background?.removeFromSuperview()

            rootView.removeFromSuperview()

            _logger.d { "VIEW HIERARCHY REMOVED" }
        } catch (e: Exception) {
            _logger.e(e) { "ERROR WHILE CLOSING WINDOW" }
        } finally {
            // Clear associated objects FIRST while rootView is active and valid
            objc_removeAssociatedObjects(rootView)
            rootView.layer?.removeAllAnimations()
            _logger.d { "DESTROY VIEW COMPLETED AND REMOVED ASSOCIATED OBJECTS" }
        }
    }

    actual override fun setBounds(viewHandle: Long, x: Int, y: Int, width: Int, height: Int) = dispatchOnMain {
        _logger.d { "SET BOUNDS CALLED : x=$x y=$y width=$width height=$height" }

        val rootView = viewHandle.toNSView() ?: return@dispatchOnMain
        val background = rootView.getBackground() ?: return@dispatchOnMain
        val container = rootView.getContainer() ?: return@dispatchOnMain
        val label = rootView.getLabel() ?: return@dispatchOnMain

        val rootFrame = cValue<NSRect> {
            origin.x = x.toDouble()
            origin.y = y.toDouble()
            size.width = width.toDouble()
            size.height = height.toDouble()
        }
        rootView.setFrame(rootFrame)
        background.setFrame(rootFrame)

        val paddingH = 16.0
        val paddingV = 8.0

        val containerFrame = cValue<NSRect> {
            origin.x = paddingH
            origin.y = paddingV
            size.width = (width - paddingH * 2).coerceAtLeast(0.0)
            size.height = (height - paddingV * 2).coerceAtLeast(0.0)
        }
        container.setFrame(containerFrame)
        val labelFrame = cValue<NSRect> {
            origin.x = 0.0
            origin.y = 0.0
            size.width = containerFrame.useContents { size.width }
            size.height = containerFrame.useContents { size.height }
        }
        label.setFrame(labelFrame)

        rootView.addDebugBorder(NSColor.redColor.colorWithAlphaComponent(.2))
        container.addDebugBorder(NSColor.greenColor.colorWithAlphaComponent(.2))
        _logger.d { "SET BOUNDS APPLIED SUCCESSFULLY" }
    }

    actual override fun setCornerRadius(viewHandle: Long, radius: Float) = dispatchOnMain {
        _logger.d { "SET CORNER RADIUS CALLED : radius=$radius" }

        val rootView = viewHandle.toNSView() ?: return@dispatchOnMain
        val background = rootView.getBackground() ?: return@dispatchOnMain

        when (background) {
            is NSGlassEffectView -> background.cornerRadius = radius.toDouble()
            else -> {
                background.wantsLayer = true
                background.layer?.apply {
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
        val background = rootView.getBackground() ?: return@dispatchOnMain

        val a = ((color shr 24) and 0xFF) / 255.0
        val r = ((color shr 16) and 0xFF) / 255.0
        val g = ((color shr 8) and 0xFF) / 255.0
        val b = (color and 0xFF) / 255.0

        val nsColor = NSColor.colorWithSRGBRed(r, g, b, a)

        when (background) {
            is NSGlassEffectView -> _logger.d { "NO COLOR FOR GLASS EFFECT VIEW" }
            else -> {
                background.wantsLayer = true
                background.layer?.backgroundColor = if (a > 0.0) nsColor.CGColor else null
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
        val background = rootView.getBackground() ?: return@dispatchOnMain
        val container = rootView.getContainer() ?: return@dispatchOnMain
        val label = rootView.getLabel() ?: return@dispatchOnMain

        val generation = ++animationGeneration

        background.layer?.removeAllAnimations()
        container.layer?.removeAllAnimations()

        container.alphaValue = 0.0
        label.stringValue = text

        _logger.d { "WINDOW ORDERED TO FRONT" }

        NSAnimationContext.runAnimationGroup(
            changes = { context ->
                context?.duration = fadeInMs / 1000.0
                context?.allowsImplicitAnimation = true
                container.animator().alphaValue = 1.0
            },
            completionHandler = {
                if (generation != animationGeneration) {
                    _logger.d { "FADE-IN INVALIDATED" }
                    return@runAnimationGroup
                }
                background.playPopIn()

                val delayNs = holdMs * 1_000_000L

                dispatch_after(
                    dispatch_time(DISPATCH_TIME_NOW, delayNs),
                    dispatch_get_main_queue(),
                ) {
                    if (generation != animationGeneration) {
                        _logger.d { "FADE-OUT CANCELLED BY NEWER TOAST" }
                        return@dispatch_after
                    }

                    _logger.d { "STARTING FADE-OUT" }
                    NSAnimationContext.runAnimationGroup(
                        changes = { context ->
                            context?.duration = fadeOutMs / 1000.0
                            context?.allowsImplicitAnimation = true
                            container.animator().alphaValue = 0.0
                        },
                        completionHandler = {
                            if (generation != animationGeneration) {
                                _logger.d { "COMPLETION INVALIDATED" }
                                return@runAnimationGroup
                            }
                            background.playPopOut()
                            container.alphaValue = 0.0
                            _logger.d { "TOAST HIDDEN" }
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

        private const val TOAST_ROOT_IDENTIFIER = "toast_root_identifier"
        private const val TOAST_BACKGROUND_IDENTIFIER = "toast_background_indentifier"
        private const val TOAST_LABEL_CONTAINER_IDENTIFIER = "toast_lable_container_identifier"
        private const val TOAST_LABEL_IDENTIFIER = "toast_label_indentifier"
    }

}
