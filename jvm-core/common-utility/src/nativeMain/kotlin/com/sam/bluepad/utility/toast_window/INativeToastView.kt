package com.sam.bluepad.utility.toast_window

interface INativeToastView {

    fun createView(parentHandle: Long): Long
    fun destroyView(viewHandle: Long)
    fun setBounds(viewHandle: Long, x: Int, y: Int, width: Int, height: Int)
    fun setCornerRadius(viewHandle: Long, radius: Float)

    fun setBackgroundColor(viewHandle: Long, color: Int)
    fun show(viewHandle: Long, text: String, fadeInMs: Int = 800, holdMs: Int = 1000, fadeOutMs: Int = 400)
}
