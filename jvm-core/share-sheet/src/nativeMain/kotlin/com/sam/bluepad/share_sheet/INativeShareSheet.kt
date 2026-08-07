package com.sam.bluepad.share_sheet

interface INativeShareSheet {
    fun shareTitleAndContent(windowHandle: Long, title: String, content: String)
    fun shareText(windowHandle: Long, text: String)
}
