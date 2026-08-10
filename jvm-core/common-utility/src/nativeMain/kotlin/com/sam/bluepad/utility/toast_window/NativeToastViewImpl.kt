package com.sam.bluepad.utility.toast_window

expect class NativeToastViewImpl : INativeToastView {
    override fun createView(parentHandle: Long): Long
    override fun destroyView(viewHandle: Long)

    override fun setBounds(viewHandle: Long, x: Int, y: Int, width: Int, height: Int)
    override fun setCornerRadius(viewHandle: Long, radius: Float)

    override fun setBackgroundColor(viewHandle: Long, color: Int)
    override fun show(viewHandle: Long, text: String, fadeInMs: Int, holdMs: Int, fadeOutMs: Int)
}
