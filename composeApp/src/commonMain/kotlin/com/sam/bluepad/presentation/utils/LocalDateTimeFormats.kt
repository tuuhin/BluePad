package com.sam.bluepad.presentation.utils


import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private val BASE_TIMESTAMP_WITH_MONTH_FORMAT
    get() = LocalDateTime.Format {
        day()
        chars(" ")
        monthNumber()
        chars(" ")
        amPmHour()
        chars(":")
        amPmMarker("AM", "PM")
    }

private val BASE_TIMESTAMP_WITH_YEAR_MONTH_FORMAT
    get() = LocalDateTime.Format {
        year()
        chars(" ")
        day()
        chars(" ")
        monthNumber()
        chars(" ")
        amPmHour()
        chars(":")
        amPmMarker("AM", "PM")
    }

fun LocalDateTime.formatToTimeStamp(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val nowInstant = Clock.System.now()
    val now = nowInstant.toLocalDateTime(timeZone)
    val target = this.toInstant(timeZone)
    val diff = nowInstant - target

    if (diff.isNegative()) return this.toString()

    return when {
        diff.inWholeMinutes < 1 -> "Just now"
        diff.inWholeMinutes < 60 -> {
            val mins = diff.inWholeMinutes
            "$mins min${if (mins > 1) "s" else ""} ago"
        }
        diff.inWholeHours < 24 -> {
            val hours = diff.inWholeHours
            "$hours hour${if (hours > 1) "s" else ""} ago"
        }
        diff.inWholeDays < 7 -> "few days ago"
        this.year == now.year -> format(BASE_TIMESTAMP_WITH_MONTH_FORMAT)
        else -> format(BASE_TIMESTAMP_WITH_YEAR_MONTH_FORMAT)
    }
}
