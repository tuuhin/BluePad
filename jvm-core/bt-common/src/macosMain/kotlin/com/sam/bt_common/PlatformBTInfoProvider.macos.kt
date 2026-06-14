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

object MacOsLogWriter: LoggerConfig{

    override val minSeverity: Severity
        get() = Severity.Debug
    override val logWriterList: List<LogWriter>
        get() = listOf(NSLogWriter())
}

@OptIn(ExperimentalForeignApi::class)
actual class PlatformBTInfoProvider : BTInfoProvider {

    private val _logger = Logger(config = MacOsLogWriter, tag = "NATIVE_MACOS")

    private val lock = NSLock()


    // lambda to be invoked when bluetooth state is
    private var _callback: ((Boolean) -> Unit)? = null


    // A central manager delegate to check the protocol changes
    val delegate: CBCentralManagerDelegateProtocol = object : NSObject(), CBCentralManagerDelegateProtocol {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            val status = when (central.state) {
                CBManagerStateResetting -> "RESETTING"
                CBManagerStatePoweredOn -> "POWER ON"
                CBManagerStateUnsupported -> "UNSUPPORTED"
                CBManagerStateUnauthorized -> "UNAUTHORIZED"
                CBManagerStatePoweredOff -> "POWER OFF"
                else -> "UNKNOWN"
            }

            _logger.d { "CURRENT BLUETOOTH STATE:$status" }

            val isActive = central.state == CBManagerStatePoweredOn
            // as we cannot use coroutines here using the lock with nslock
            lock.use { _callback?.invoke(isActive) }
        }
    }


    // when the class is prepared we are able to receive the bluetooth
    // device info until its being cleared off by the underlying gc
    val centralManager by lazy {
        CBCentralManager(
            delegate = delegate,
            queue = dispatch_get_main_queue(),
            options = mapOf(CBCentralManagerOptionShowPowerAlertKey to NSNumber.numberWithBool(true)),
        )
    }


    actual override fun registerCallback(callback: (Boolean) -> Unit): Long {
        _callback = callback

        _logger.d { "REGISTERING CALLBACK" }

        // Trigger initial callback if state is already known
        val currentState = centralManager.state
        if (currentState != CBManagerStateUnknown && currentState != CBManagerStateResetting) {
            callback(currentState == CBManagerStatePoweredOn)
        }
        return 0L
    }

    actual override fun unregisterCallback(caller: Long) {

        _logger.d { "CLEARING CALLBACK" }
        _callback = null
    }

    actual override fun isBluetoothActive(): Boolean {
        val isActive = centralManager.state == CBManagerStatePoweredOn
        _logger.i { "READING BLUETOOTH STATE :$isActive" }
        return isActive
    }

    actual override fun isLEConnectionAllowed(): Boolean = true
    actual override fun isPeripheralRoleSupported(): Boolean = true
}

private inline fun <T> NSLock.use(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
