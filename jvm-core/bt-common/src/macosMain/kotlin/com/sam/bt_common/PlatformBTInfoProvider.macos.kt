package com.sam.bt_common

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AppKit.NSWorkspace
import platform.AppKit.openURL
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerOptionShowPowerAlertKey
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateResetting
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnknown
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.Foundation.NSDictionary
import platform.Foundation.NSLock
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.dictionaryWithDictionary
import platform.Foundation.numberWithBool
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

private class ProbeRetainer(
    var manager: CBCentralManager? = null,
    var delegate: CBCentralManagerDelegateProtocol? = null
)

@OptIn(ExperimentalForeignApi::class)
actual class PlatformBTInfoProvider : BTInfoProvider {

    private val _logger = Logger(config = MacOsLogWriter, tag = "NATIVE_MACOS_BT_COMMON")
    private val lock = NSLock()

    private var _centralManager: CBCentralManager? = null
    private var _centralDelegate: CBCentralManagerDelegateProtocol? = null

    actual override fun requestBTEnable(): Int {
        // status code -1 means request option not present
        // user need to open settings to active
        return -1
    }

    actual override fun openBTSettings() {
        val url = NSURL.URLWithString("x-apple.systempreferences:com.apple.preferences.Bluetooth")
            ?: return
        NSWorkspace.sharedWorkspace.openURL(url)
    }

    actual override val canActivateBTFromApp: Boolean = false
    actual override val canRequestOpenSettings: Boolean = true

    actual override fun registerCallback(callback: (Boolean) -> Unit): Long = lock.use {
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

        val options = NSDictionary.dictionaryWithDictionary(
            mapOf(CBCentralManagerOptionShowPowerAlertKey to NSNumber.numberWithBool(false)),
        )

        val centralManager = CBCentralManager(
            delegate = delegate,
            queue = dispatch_get_main_queue(),
            options = options,
        )

        _centralManager = centralManager
        _centralDelegate = delegate

        _logger.d { "CENTRAL MANAGER IS SUCCESSFULLY SET" }

        val currentState = centralManager.state
        if (currentState != CBManagerStateUnknown && currentState != CBManagerStateResetting) {
            // responding if state is already updated
            callback(currentState == CBManagerStatePoweredOn)
        }
        return 0L
    }

    actual override fun unregisterCallback(caller: Long) = lock.use {
        _logger.d { "CLEARING CENTRAL MANAGER INSTANCE" }
        _centralDelegate = null
        _centralManager = null
    }

    actual override suspend fun isBluetoothActive(): Boolean {
        lock.use {
            val manager = _centralManager ?: return@use
            val isInvalidState = manager.state == CBManagerStateUnknown || manager.state == CBManagerStateResetting
            if (!isInvalidState) return@use
            _logger.d { "CACHED PROBE FOUND RETURNING VALUE FROM PROBE" }
            return manager.state == CBManagerStatePoweredOn
        }
        return suspendCancellableCoroutine { cont ->
            val retainer = ProbeRetainer()

            val tempDelegate = object : NSObject(), CBCentralManagerDelegateProtocol {
                override fun centralManagerDidUpdateState(central: CBCentralManager) {
                    if (central.state == CBManagerStateUnknown || central.state == CBManagerStateResetting) return

                    if (cont.isActive) {
                        _logger.d { "CENTRAL MANAGER STATE FROM COROUTINE :${central.bluetoothStateAsString()}" }
                        cont.resume(central.state == CBManagerStatePoweredOn)
                    }
                    _logger.d { "CLEANING PROBE DATA" }
                    retainer.manager = null
                    retainer.delegate = null
                }
            }

            val options = NSDictionary.dictionaryWithDictionary(
                mapOf(CBCentralManagerOptionShowPowerAlertKey to NSNumber.numberWithBool(false)),
            )

            retainer.delegate = tempDelegate
            retainer.manager = CBCentralManager(
                delegate = tempDelegate,
                queue = dispatch_get_main_queue(),
                options = options,
            )

            cont.invokeOnCancellation {
                _logger.d { "CENTRAL MANAGER CANCELLED AND CLEANED" }
                retainer.manager = null
                retainer.delegate = null
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
