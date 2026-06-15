package com.sam.bt_common

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.NSLogWriter
import co.touchlab.kermit.Severity
import platform.CoreBluetooth.*
import platform.darwin.*
import platform.Foundation.*
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private object MacOsLogWriter : LoggerConfig {

    override val minSeverity: Severity
        get() = Severity.Debug
    override val logWriterList: List<LogWriter>
        get() = listOf(NSLogWriter())
}

@OptIn(ExperimentalForeignApi::class)
actual class PlatformBTInfoProvider : BTInfoProvider {

    private val _logger = Logger(config = MacOsLogWriter, tag = "NATIVE_MACOS_BT_COMMON")

    private val lock = NSLock()

    private var _centralManager: CBCentralManager? = null
    private var _centralDelegate: CBCentralManagerDelegateProtocol? = null

    actual override fun registerCallback(callback: (Boolean) -> Unit): Long {
        if (_centralManager != null) {
            _logger.d { "CENTRAL MANAGER IS ALREADY REGISTERED UNREGISTER THIS TO CONTINUE" }
            return -1L
        }

        val delegate = object : NSObject(), CBCentralManagerDelegateProtocol {
            override fun centralManagerDidUpdateState(central: CBCentralManager) {
                _logger.d { "CURRENT BLUETOOTH STATE:${central.bluetoothStateAsString()}" }
                val isActive = central.state == CBManagerStatePoweredOn
                callback(isActive)
            }
        }

        val centralManager = CBCentralManager(
            delegate = delegate,
            queue = dispatch_get_main_queue(),
            options = mapOf(CBCentralManagerOptionShowPowerAlertKey to NSNumber.numberWithBool(true)),
        )

        _centralManager = centralManager
        _centralDelegate = delegate

        _logger.d { "CENTRAL MANAGER IS SET" }

        val currentState = centralManager.state
        if (currentState != CBManagerStateUnknown && currentState != CBManagerStateResetting) {
            callback(currentState == CBManagerStatePoweredOn)
        }
        return 0L
    }

    actual override fun unregisterCallback(caller: Long) = lock.use {
        _logger.d { "CLEARING CENTRAL MANAGER INSTANCE" }
        _centralManager = null
        _centralDelegate = null
    }

    actual override suspend fun isBluetoothActive(): Boolean {
        return suspendCancellableCoroutine { cont ->

            var manager: CBCentralManager? = null
            var delegate: CBCentralManagerDelegateProtocol? = null

            val tempDelegate = object : NSObject(), CBCentralManagerDelegateProtocol {
                override fun centralManagerDidUpdateState(central: CBCentralManager) {
                    if (central.state == CBManagerStateUnknown || central.state == CBManagerStateResetting) return

                    if (cont.isActive) {
                        _logger.d { "CENTRAL MANAGER STATE FROM COROUTINE :${central.bluetoothStateAsString()}" }
                        cont.resume(central.state == CBManagerStatePoweredOn)
                        manager = null
                        delegate = null
                    }
                }
            }

            val probeManager = CBCentralManager(
                delegate = tempDelegate,
                queue = dispatch_get_main_queue(),
                options = mapOf(CBCentralManagerOptionShowPowerAlertKey to NSNumber.numberWithBool(false)),
            )

            // Bind them together in a local scope holder
            manager = probeManager
            delegate = tempDelegate
            _logger.d { "CENTRAL MANAGER BLUETOOTH STATE WILL RESPOND SOON" }

            cont.invokeOnCancellation {
                _logger.d { "CENTRAL MANAGER CANCELLED AND CLEANED" }
                manager = null
                delegate = null
            }
        }
    }

    actual override fun isLEConnectionAllowed(): Boolean = true
    actual override fun isPeripheralRoleSupported(): Boolean = true

    private fun CBCentralManager.bluetoothStateAsString() = when (state) {
        CBManagerStateResetting -> "RESETTING"
        CBManagerStatePoweredOn -> "POWER ON"
        CBManagerStateUnsupported -> "UNSUPPORTED"
        CBManagerStateUnauthorized -> "UNAUTHORIZED"
        CBManagerStatePoweredOff -> "POWER OFF"
        else -> "UNKNOWN"
    }
}

private inline fun <T> NSLock.use(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
