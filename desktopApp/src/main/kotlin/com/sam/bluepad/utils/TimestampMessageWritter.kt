package com.sam.bluepad.utils

import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal object TimestampMessageWriter : MessageStringFormatter {

    override fun formatMessage(severity: Severity?, tag: Tag?, message: Message): String {
        val currentMoment = Clock.System.now()
        val timestamp = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
            .format(
                LocalDateTime.Format {
                    hour()
                    chars(":")
                    minute()
                    chars(":")
                    second()
                },
            )
        val threadName = Thread.currentThread().name

        // Build the structured log line
        val severityStr = severity?.let { "${it.name}: " } ?: ""
        val tagStr = tag?.tag ?: ""
        val messageText = message.message

        return "[$timestamp] [$threadName] [$tagStr] $severityStr$messageText"
    }
}
