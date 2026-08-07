package com.sam.bluepad.share_sheet

expect class NativeShareSheetImpl : INativeShareSheet {
    override fun shareTitleAndContent(windowHandle: Long, title: String, content: String)
    override fun shareText(windowHandle: Long, text: String)
}
