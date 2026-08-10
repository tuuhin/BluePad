package com.sam.bluepad.share_sheet

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.Severity
import com.sam.bluepad.shareSheet.mingw.open_share_sheet_text
import com.sam.bluepad.shareSheet.mingw.open_share_sheet_title_and_desc
import kotlinx.cinterop.toCPointer
import platform.windows.HWND
import platform.windows.IsWindow
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
actual class NativeShareSheetImpl : INativeShareSheet {

    actual override fun shareTitleAndContent(windowHandle: Long, title: String, content: String) {
        _logger.i { "REQUESTED WINDOW HANDLE :$windowHandle" }
        val hWindow: HWND = windowHandle.toCPointer() ?: run {
            _logger.w { "DESTROY VIEW FAILED: invalid pointer conversion for handle=$windowHandle" }
            return
        }

        if (IsWindow(hWindow) == 0) {
            _logger.w { "HWND INSTANCE IS NOT AN WINDOW" }
            return
        }

        _logger.d { "OPENING SHARE SHEET" }
        open_share_sheet_title_and_desc(hWindow, title, content)
    }

    actual override fun shareText(windowHandle: Long, text: String) {
        _logger.i { "REQUESTED WINDOW HANDLE :$windowHandle" }
        val hWindow: HWND = windowHandle.toCPointer() ?: run {
            _logger.w { "DESTROY VIEW FAILED: invalid pointer conversion for handle=$windowHandle" }
            return
        }

        if (IsWindow(hWindow) == 0) {
            _logger.w { "HWND INSTANCE IS NOT AN WINDOW" }
            return
        }

        _logger.d { "OPENING SHARE SHEET" }
        open_share_sheet_text(hWindow, text)
    }

    companion object {
        private val _logger = Logger(
            tag = "NATIVE_SHARE_SHEET",
            config = object : LoggerConfig {
                override val minSeverity: Severity = Severity.Debug
                override val logWriterList: List<LogWriter> = listOf(CommonWriter())
            },
        )
    }
}
