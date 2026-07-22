package com.sam.bt_common

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.NSLogWriter
import co.touchlab.kermit.Severity

internal object MacOsLogWriter : LoggerConfig {
    override val minSeverity: Severity
        get() = Severity.Debug

    override val logWriterList: List<LogWriter>
        get() = listOf(NSLogWriter())
}
