package com.sam.bluepad.utility.toast_window

import com.sam.bluepad.common_utils.ToastStateCStruct
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCPointer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

internal enum class Phase { Idle, FadeIn, Hold, FadeOut }

internal class ToastStatePtr private constructor(private val ptr: CPointer<ToastStateCStruct>) {

    private val struct: ToastStateCStruct = ptr.pointed

    var animationRef: Long by struct.delegate(ToastStateCStruct::animation_ref)
    var backgroundColor: UInt by struct.delegate(ToastStateCStruct::background_color)
    var fadeInMs: UInt by struct.delegateUShort(ToastStateCStruct::fade_in_ms)
    var holdMs: UInt by struct.delegateUShort(ToastStateCStruct::hold_ms)
    var fadeOutMs: UInt by struct.delegateUShort(ToastStateCStruct::fade_out_ms)
    var elapsedMs: UInt by struct.delegateUShort(ToastStateCStruct::elapsed_ms)
    var alpha: UByte by struct.delegate(ToastStateCStruct::alpha)
    var phase: Phase by struct.delegateEnum(ToastStateCStruct::phase)

    fun free() = nativeHeap.free(ptr)

    fun rawAddress(): Long = ptr.rawValue.toLong()

    inline fun advance(interval: Int = 30, onHoldComplete: () -> Unit): Boolean {
        elapsedMs += interval.toUInt()
        when (phase) {
            Phase.Hold -> {
                if (elapsedMs >= holdMs) {
                    phase = Phase.FadeOut
                    elapsedMs = 0u
                    onHoldComplete()
                }
            }

            else -> return false
        }
        return true
    }

    companion object {
        fun allocate(): ToastStatePtr {
            val ptr = nativeHeap.alloc<ToastStateCStruct>().ptr

            return ToastStatePtr(ptr).apply {
                phase = Phase.Idle
                alpha = 255u
                fadeInMs = 200u
                holdMs = 1000u
                fadeOutMs = 200u
                elapsedMs = 0u
                backgroundColor = 0x00000000u
                animationRef = 0L
            }
        }

        fun at(address: Long): ToastStatePtr? {
            if (address == 0L) return null
            val ptr = address.toCPointer<ToastStateCStruct>() ?: return null
            return ToastStatePtr(ptr)
        }
    }
}

private fun <S, T> S.delegate(prop: KMutableProperty1<S, T>) = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = prop.get(this@delegate)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = prop.set(this@delegate, value)
}

private fun <S> S.delegateUShort(prop: KMutableProperty1<S, UShort>) = object : ReadWriteProperty<Any?, UInt> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): UInt = prop.get(this@delegateUShort).toUInt()
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: UInt) =
        prop.set(this@delegateUShort, value.toUShort())
}

private inline fun <S, reified E : Enum<E>> S.delegateEnum(prop: KMutableProperty1<S, UByte>) =
    object : ReadWriteProperty<Any?, E> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): E {
            val raw = prop.get(this@delegateEnum).toInt()
            return enumValues<E>().getOrElse(raw) { enumValues<E>()[0] }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: E) {
            prop.set(this@delegateEnum, value.ordinal.toUByte())
        }
    }
