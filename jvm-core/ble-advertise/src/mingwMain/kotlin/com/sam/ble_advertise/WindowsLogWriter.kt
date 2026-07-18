package com.sam.ble_advertise

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import platform.windows.DWORD
import platform.windows.DWORDVar
import platform.windows.GetConsoleMode
import platform.windows.GetCurrentThreadId
import platform.windows.GetStdHandle
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.STD_OUTPUT_HANDLE
import platform.windows.SetConsoleMode
import kotlin.time.Clock

internal fun enableWindowsAnsiColors() = memScoped {
    val hOut = GetStdHandle(STD_OUTPUT_HANDLE)
    if (hOut == INVALID_HANDLE_VALUE) return@memScoped

    val dwMode = alloc<DWORDVar>()
    if (GetConsoleMode(hOut, dwMode.ptr) != 0) {
        val enableVirtualTerminalProcessing: DWORD = 0x0004u
        val newMode = dwMode.value or enableVirtualTerminalProcessing
        SetConsoleMode(hOut, newMode)
    }
}

private object TimestampMessageWriter : MessageStringFormatter {

    private val format = LocalDateTime.Format {
        hour()
        chars(":")
        minute()
        chars(":")
        second()
    }

    override fun formatMessage(severity: Severity?, tag: Tag?, message: Message): String {
        val currentMoment = Clock.System.now()
        val timestamp = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
            .format(format)

        val threadId = GetCurrentThreadId().toString()

        val severityStr = severity?.let { "${it.name}: " } ?: ""
        val tagStr = tag?.tag ?: ""
        val messageText = message.message

        return "[$timestamp] [$threadId] [$tagStr] $severityStr$messageText"
    }
}

internal class WindowsLogWriter(
    private val formatter: MessageStringFormatter = TimestampMessageWriter,
    private val enabled: Boolean = false,
) : LogWriter() {

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (!enabled) return
        val formattedMessage = formatter.formatMessage(severity, Tag(tag), Message(message))

        val colorCode = getColorForSeverity(severity)
        val resetCode = "\u001B[0m"

        println("$colorCode$formattedMessage$resetCode")
        throwable?.printStackTrace()
    }

    private fun getColorForSeverity(severity: Severity): String = when (severity) {
        Severity.Debug -> "\u001B[36m" // Cyan
        Severity.Info -> "\u001B[32m" // Green
        Severity.Warn -> "\u001B[33m" // Yellow
        Severity.Error -> "\u001B[31m" // Red
        else -> "\u001B[37m" // White / Light Gray
    }
}
