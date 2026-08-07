package com.sam.bluepad.utils

import com.sam.bluepad.platform.common_utils.NativeToastViewImpl
import dev.nucleusframework.window.tao.NucleusPlatformView
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class WindowsNativeToastView(private val parent: Long) : NucleusPlatformView.HWnd {

    private val instance = NativeToastViewImpl()

    override val hwndHandle: Long by lazy { instance.createView(parent) }

    override fun setBounds(xPx: Int, yPx: Int, widthPx: Int, heightPx: Int) =
        instance.setBounds(hwndHandle, xPx, yPx, widthPx, heightPx)

    override fun setCornerRadius(radiusPx: Float) = instance.setCornerRadius(hwndHandle, radiusPx)

    override fun dispose() {
        instance.destroyView(hwndHandle)
        instance.close()
    }

    fun setBackground(color: Int) = instance.setBackgroundColor(viewHandle = hwndHandle, color = color)

    fun show(
        text: String,
        fadeInDuration: Duration = 200.milliseconds,
        holdDuration: Duration = 1200.milliseconds,
        fadeoutMs: Duration = 200.milliseconds
    ) = instance.show(
        viewHandle = hwndHandle,
        text = text,
        fadeInMs = fadeInDuration.inWholeMilliseconds.toInt(),
        holdMs = holdDuration.inWholeMilliseconds.toInt(),
        fadeOutMs = fadeoutMs.inWholeMilliseconds.toInt(),
    )
}

internal class MacOSXNativeToastView(val parent: Long) : NucleusPlatformView.NsView {

    private val instance = NativeToastViewImpl()

    override val nsViewHandle: Long by lazy { instance.createView(parent) }

    override fun setBounds(xPx: Int, yPx: Int, widthPx: Int, heightPx: Int) =
        instance.setBounds(nsViewHandle, xPx, yPx, widthPx, heightPx)

    override fun setCornerRadius(radiusPx: Float) = instance.setCornerRadius(nsViewHandle, radiusPx)

    override fun dispose() {
        instance.destroyView(nsViewHandle)
        instance.close()
    }

    fun show(
        text: String,
        fadeInDuration: Duration = 200.milliseconds,
        holdDuration: Duration = 1200.milliseconds,
        fadeoutMs: Duration = 200.milliseconds
    ) = instance.show(
        viewHandle = nsViewHandle,
        text = text,
        fadeInMs = fadeInDuration.inWholeMilliseconds.toInt(),
        holdMs = holdDuration.inWholeMilliseconds.toInt(),
        fadeOutMs = fadeoutMs.inWholeMilliseconds.toInt(),
    )
}
