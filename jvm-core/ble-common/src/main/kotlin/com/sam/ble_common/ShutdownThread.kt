package com.sam.ble_common

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

class ShutdownThread(private val cleanUp: () -> Unit = {}) : Thread("shut-down-thread") {
	override fun run() {
		try {
			cleanUp()
			getAllStackTraces().keys.forEach { t -> Logger.d { "NAME :${t?.name} IS_DEMON= ${t.isDaemon} IS_ALIVE:${t.isAlive}" } }

			Logger.i { "JVM shutdown initiated at ${System.currentTimeMillis()}" }
		} finally {
			Logger.i { "JVM SHUT DOWN COMPLETE" }
		}
	}

	override fun start() {
		Logger.setTag("SHUTDOWN THREAD")
		Logger.setMinSeverity(Severity.Debug)
		super.start()
	}
}