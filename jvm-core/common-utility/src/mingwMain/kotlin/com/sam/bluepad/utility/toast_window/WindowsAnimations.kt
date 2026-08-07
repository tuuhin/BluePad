package com.sam.bluepad.utility.toast_window

import com.sam.bluepad.common_utils.CLSID_UIAnimationManager
import com.sam.bluepad.common_utils.CLSID_UIAnimationTransitionLibrary
import com.sam.bluepad.common_utils.IID_IUIAnimationManager
import com.sam.bluepad.common_utils.IID_IUIAnimationTransitionLibrary
import com.sam.bluepad.common_utils.IUIAnimationManager
import com.sam.bluepad.common_utils.IUIAnimationStoryboard
import com.sam.bluepad.common_utils.IUIAnimationTransition
import com.sam.bluepad.common_utils.IUIAnimationTransitionLibrary
import com.sam.bluepad.common_utils.IUIAnimationVariable
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.windows.CLSCTX_INPROC_SERVER
import platform.windows.COINIT_APARTMENTTHREADED
import platform.windows.CoCreateInstance
import platform.windows.CoInitializeEx
import platform.windows.CoUninitialize
import platform.windows.GetTickCount64

class WindowsAnimations {

    private var _manager: CPointer<IUIAnimationManager>? = null
    private var _transitionLib: CPointer<IUIAnimationTransitionLibrary>? = null
    private var _alphaAnimVariable: CPointer<IUIAnimationVariable>? = null

    fun initialize() = memScoped {
        CoInitializeEx(null, COINIT_APARTMENTTHREADED)

        val manager = alloc<CPointerVar<IUIAnimationManager>>()
        CoCreateInstance(
            CLSID_UIAnimationManager.ptr,
            null,
            CLSCTX_INPROC_SERVER.toUInt(),
            IID_IUIAnimationManager.ptr,
            manager.ptr.reinterpret(),
        )
        _manager = manager.value

        val transitionLibPtr = alloc<CPointerVar<IUIAnimationTransitionLibrary>>()
        CoCreateInstance(
            CLSID_UIAnimationTransitionLibrary.ptr,
            null,
            CLSCTX_INPROC_SERVER.toUInt(),
            IID_IUIAnimationTransitionLibrary.ptr,
            transitionLibPtr.ptr.reinterpret(),
        )
        _transitionLib = transitionLibPtr.value

        val varPtr = alloc<CPointerVar<IUIAnimationVariable>>()
        val newAnimationVariable = _manager?.pointed?.lpVtbl?.pointed ?: return@memScoped
        val variable = newAnimationVariable.CreateAnimationVariable ?: return@memScoped
        variable.invoke(_manager, 0.0, varPtr.ptr)
        _alphaAnimVariable = varPtr.value
    }

    fun animateTo(targetValue: Double, durationSeconds: Double = 0.2) {
        val manager = _manager ?: return
        val library = _transitionLib ?: return
        val variable = _alphaAnimVariable ?: return

        memScoped {
            val storyboardPtr = alloc<CPointerVar<IUIAnimationStoryboard>>()
            manager.pointed.lpVtbl?.pointed?.CreateStoryboard?.invoke(manager, storyboardPtr.ptr)
            val storyboard = storyboardPtr.value ?: return

            val transitionPtr = alloc<CPointerVar<IUIAnimationTransition>>()
            library.pointed.lpVtbl?.pointed?.CreateAccelerateDecelerateTransition?.invoke(
                library, durationSeconds, targetValue, 0.2, 0.2, transitionPtr.ptr,
            )
            try {
                val transition = transitionPtr.value ?: return

                storyboard.pointed.lpVtbl?.pointed?.AddTransition?.invoke(
                    storyboard,
                    variable,
                    transition,
                )

                val currentTime = GetTickCount64().toDouble() / 1000.0
                storyboard.pointed.lpVtbl?.pointed?.Schedule?.invoke(
                    storyboard,
                    currentTime,
                    null,
                )
                transition.pointed.lpVtbl?.pointed?.Release?.invoke(transition)
            } finally {
                storyboard.pointed.lpVtbl?.pointed?.Release?.invoke(storyboard)
            }
        }
    }

    fun getCurrentAlpha(): Double {
        val manager = _manager ?: return 0.0
        val variable = _alphaAnimVariable ?: return 0.0

        memScoped {
            val currentTime = GetTickCount64().toDouble() / 1000.0
            manager.pointed.lpVtbl?.pointed?.Update?.invoke(manager, currentTime, null)

            val valPtr = alloc<DoubleVar>()
            variable.pointed.lpVtbl?.pointed?.GetValue?.invoke(variable, valPtr.ptr)
            return valPtr.value
        }
    }

    fun onDestroy() {
        if (_alphaAnimVariable != null) {
            _alphaAnimVariable!!.pointed.lpVtbl?.pointed?.Release?.invoke(_alphaAnimVariable!!)
            _alphaAnimVariable = null
        }
        if (_transitionLib != null) {
            _transitionLib!!.pointed.lpVtbl?.pointed?.Release?.invoke(_transitionLib!!)
            _transitionLib = null
        }
        if (_manager != null) {
            _manager!!.pointed.lpVtbl?.pointed?.Release?.invoke(_manager!!)
            _manager = null

        }
        CoUninitialize()
    }
}
