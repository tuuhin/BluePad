package com.sam.bluepad.utility.toast_window

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.free
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.set
import kotlinx.cinterop.toCPointer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal enum class Phase { Idle, FadeIn, Hold, FadeOut }

internal class ToastStatePtr private constructor(private val ptr: CPointer<IntVar>) {

    var phase: Phase by ptrEnum(0, Phase.Idle)
    var alpha: Int by ptrInt(1)
    var fadeInMs: Int by ptrInt(2)
    var holdMs: Int by ptrInt(3)
    var fadeOutMs: Int by ptrInt(4)
    var elapsedMs: Int by ptrInt(5)
    var backgroundColor: UInt by ptrUInt(6)

    fun free() = nativeHeap.free(ptr)

    fun rawAddress(): Long = ptr.rawValue.toLong()

    private fun ptrInt(index: Int) = object : ReadWriteProperty<ToastStatePtr, Int> {
        override fun getValue(thisRef: ToastStatePtr, property: KProperty<*>): Int = ptr[index]
        override fun setValue(thisRef: ToastStatePtr, property: KProperty<*>, value: Int) {
            ptr[index] = value
        }
    }

    private fun ptrUInt(index: Int) = object : ReadWriteProperty<ToastStatePtr, UInt> {
        override fun getValue(thisRef: ToastStatePtr, property: KProperty<*>): UInt = ptr[index].toUInt()
        override fun setValue(thisRef: ToastStatePtr, property: KProperty<*>, value: UInt) {
            ptr[index] = value.toInt()
        }
    }

    private inline fun <reified E : Enum<E>> ptrEnum(index: Int, default: E) =
        object : ReadWriteProperty<ToastStatePtr, E> {
            override fun getValue(thisRef: ToastStatePtr, property: KProperty<*>): E {
                return enumValues<E>().getOrElse(ptr[index]) { default }
            }

            override fun setValue(thisRef: ToastStatePtr, property: KProperty<*>, value: E) {
                ptr[index] = value.ordinal
            }
        }

    fun advance(interval: Int = 30): Boolean {
        elapsedMs += interval

        when (phase) {
            Phase.FadeIn -> {
                val t = if (fadeInMs > 0) elapsedMs.toFloat() / fadeInMs else 1f
                if (t >= 1f) {
                    alpha = 255
                    phase = Phase.Hold
                    elapsedMs = 0
                } else {
                    alpha = (255 * t).toInt()
                }
            }

            Phase.Hold -> {
                if (elapsedMs >= holdMs) {
                    phase = Phase.FadeOut
                    elapsedMs = 0
                }
            }

            Phase.FadeOut -> {
                val t = if (fadeOutMs > 0) elapsedMs.toFloat() / fadeOutMs else 1f
                if (t >= 1f) {
                    alpha = 0
                    phase = Phase.Idle
                    return false
                } else {
                    alpha = (255 * (1f - t)).toInt()
                }
            }

            Phase.Idle -> return false
        }
        return true
    }

    companion object {

        private const val FIELD_COUNT = 6

        fun allocate(): ToastStatePtr {
            val ptr = nativeHeap.allocArray<IntVar>(FIELD_COUNT)
            for (i in 0 until FIELD_COUNT) ptr[i] = 0
            return ToastStatePtr(ptr).apply {
                phase = Phase.Idle
                fadeInMs = 200
                holdMs = 1000
                fadeOutMs = 200
                backgroundColor = 0x000000u
            }
        }

        fun at(address: Long): ToastStatePtr? {
            if (address == 0L) return null
            val ptr = address.toCPointer<IntVar>() ?: return null
            return ToastStatePtr(ptr)
        }
    }
}
