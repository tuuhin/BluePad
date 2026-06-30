package com.sam.bt_common

import com.sam.bt_common.platform.mingw.bond_manager_accept_connection
import com.sam.bt_common.platform.mingw.bond_manager_reject_connection
import com.sam.bt_common.platform.mingw.bond_manager_request_pairing
import com.sam.bt_common.platform.mingw.bond_manager_unregister_pairing
import com.sam.bt_common.platform.mingw.bt_bond_pairing_callback
import com.sam.bt_common.platform.mingw.create_bond_manager
import com.sam.bt_common.platform.mingw.destroy_bond_manager
import com.sam.bt_common.platform.mingw.is_device_bonded
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readValue
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKString
import kotlinx.cinterop.toLong
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private data class MingwBondCallback(
    val onConfirmPin: (pin: String) -> Unit, val onResponse: (Int) -> Unit, val onError: (Int) -> Unit
)

@OptIn(
    ExperimentalAtomicApi::class,
)
actual class PlatformBondInfoProvider : BTBondInfoProvider {

    actual override val canReadBondInfo: Boolean = true
    actual override val canShowConfirmPinDialog: Boolean = true

    actual override fun checkDeviceBondState(address: String): Int {
        return is_device_bonded(address)
    }

    actual override fun registerForBondConfirmPin(
        address: String, onConfirmPin: (pin: String) -> Unit, onResponse: (Int) -> Unit, onError: (Int) -> Unit
    ) {
        _callbackRef?.dispose()

        val instance = MingwBondCallback(onError = onError, onConfirmPin = onConfirmPin, onResponse = onResponse)
        _callbackRef = StableRef.create(instance)

        if (_btManagerHandle.load() > 0) {
            // destroy everything first if any present
            unregisterForBondConfirmPin()
        }

        // create a new handle and store the value
        val handle = create_bond_manager()
        _btManagerHandle.store(handle.toLong())

        memScoped {
            val callback = alloc<bt_bond_pairing_callback>().apply {
                on_confirm_pin = staticCFunction { pin, d ->
                    if (pin == null) return@staticCFunction
                    _btBondResponseHandle.store(d.toLong())
                    _callbackRef?.get()?.onConfirmPin?.invoke(pin.toKString())
                }

                on_error = staticCFunction { code ->
                    _callbackRef?.get()?.onError?.invoke(code)
                }
                on_results = staticCFunction { code ->
                    _callbackRef?.get()?.onResponse?.invoke(code)
                }
            }

            val handle = _btManagerHandle.load()
            bond_manager_request_pairing(
                handle = handle.toCPointer(),
                device_address = address,
                callback = callback.readValue(),
            )
        }
    }

    actual override fun acceptConfirmPin(pin: String) {
        if (_btManagerHandle.load() <= 0L) throw IllegalStateException("Need to register an handle to accept confirmation")
        if (_btBondResponseHandle.load() <= 0L) throw IllegalStateException("No confirm pin response made to accept")

        // otherwise call accept connection with pointers
        bond_manager_accept_connection(
            handle = _btManagerHandle.load().toCPointer(),
            pin = pin,
            responder = _btBondResponseHandle.load().toCPointer(),
        )
        // if everything goes will handle needs to be reset
        _btBondResponseHandle.store(0L)
    }

    actual override fun unregisterForBondConfirmPin() {
        if (_btManagerHandle.load() <= 0L) throw IllegalStateException("Need to register an handle to accept confirmation")

        val ptr = _btManagerHandle.load()

        // unregister for pairing
        bond_manager_unregister_pairing(ptr.toCPointer())

        if (_btBondResponseHandle.load() > 0L) {
            // if the pointer is still open reject it
            bond_manager_reject_connection(
                handle = _btManagerHandle.load().toCPointer(),
                responder = _btBondResponseHandle.load().toCPointer(),
            )
            _btBondResponseHandle.store(0L)
        }

        // destroy the bond manager
        destroy_bond_manager(handle = ptr.toCPointer())
        _btManagerHandle.store(0L)

        // dispose the callback
        _callbackRef?.dispose()
        _callbackRef = null
    }

    companion object {
        private val _btManagerHandle = AtomicLong(-1L)
        private val _btBondResponseHandle = AtomicLong(-1L)
        private var _callbackRef: StableRef<MingwBondCallback>? = null
    }

}
