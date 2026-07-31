package com.sam.bluepad.toast

import com.sam.bluepad.platform.common_utils.NativeToastViewImpl
import dev.nucleusframework.window.tao.NucleusPlatformView

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

    fun show(text: String) = instance.show(hwndHandle, text = text, 200, 1500, 200)
}

internal class MacOSXNativeToastView : NucleusPlatformView.NsView {

    private val instance = NativeToastViewImpl()

    override val nsViewHandle: Long by lazy { instance.createView(0L) }

    override fun setBounds(xPx: Int, yPx: Int, widthPx: Int, heightPx: Int) =
        instance.setBounds(nsViewHandle, xPx, yPx, widthPx, heightPx)

    override fun setCornerRadius(radiusPx: Float) = instance.setCornerRadius(nsViewHandle, radiusPx)

    override fun dispose() {
        instance.destroyView(nsViewHandle)
        instance.close()
    }

    fun show(text: String) = instance.show(nsViewHandle, text = text, 200, 1500, 200)
}
