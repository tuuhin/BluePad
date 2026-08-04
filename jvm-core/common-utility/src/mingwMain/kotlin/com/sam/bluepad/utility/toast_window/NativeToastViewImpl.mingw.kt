package com.sam.bluepad.utility.toast_window

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.Severity
import com.sam.bluepad.utility.WindowsLogWriter
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.wcstr
import platform.windows.BeginPaint
import platform.windows.COLORREF
import platform.windows.CS_DROPSHADOW
import platform.windows.CreateSolidBrush
import platform.windows.CreateWindowExW
import platform.windows.DT_CENTER
import platform.windows.DT_SINGLELINE
import platform.windows.DT_VCENTER
import platform.windows.DefWindowProcW
import platform.windows.DeleteObject
import platform.windows.DestroyWindow
import platform.windows.DrawTextW
import platform.windows.EndPaint
import platform.windows.FillRect
import platform.windows.GWLP_USERDATA
import platform.windows.GetClientRect
import platform.windows.GetModuleHandleW
import platform.windows.GetWindowLongPtrW
import platform.windows.GetWindowTextW
import platform.windows.HDC__
import platform.windows.HINSTANCE
import platform.windows.HWND
import platform.windows.IDC_ARROW
import platform.windows.IsWindow
import platform.windows.KillTimer
import platform.windows.LPARAM
import platform.windows.LWA_ALPHA
import platform.windows.LoadCursor
import platform.windows.PAINTSTRUCT
import platform.windows.RDW_INVALIDATE
import platform.windows.RDW_UPDATENOW
import platform.windows.RECT
import platform.windows.RedrawWindow
import platform.windows.RegisterClassExW
import platform.windows.SWP_NOACTIVATE
import platform.windows.SWP_NOZORDER
import platform.windows.SW_HIDE
import platform.windows.SW_SHOWNOACTIVATE
import platform.windows.S_OK
import platform.windows.SelectObject
import platform.windows.SetBkMode
import platform.windows.SetLayeredWindowAttributes
import platform.windows.SetTextColor
import platform.windows.SetTimer
import platform.windows.SetWindowLongPtrW
import platform.windows.SetWindowPos
import platform.windows.SetWindowTextW
import platform.windows.ShowWindow
import platform.windows.TRANSPARENT
import platform.windows.UINT
import platform.windows.WM_ERASEBKGND
import platform.windows.WM_NCDESTROY
import platform.windows.WM_PAINT
import platform.windows.WM_TIMER
import platform.windows.WNDCLASSEXW
import platform.windows.WPARAM
import platform.windows.WS_EX_LAYERED
import platform.windows.WS_EX_NOACTIVATE
import platform.windows.WS_EX_TOOLWINDOW
import platform.windows.WS_POPUP
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
actual class NativeToastViewImpl : INativeToastView {

    actual override fun createView(parentHandle: Long): Long {

        _logger.v { "CREATE VIEW CALLED" }

        val instance = GetModuleHandleW(lpModuleName = null) ?: run {
            _logger.w { "CANNOT CREATE A VIEW  GetModuleHandleW returned null" }
            return -1L
        }
        ensureClassRegistered(instance)

        val parentWindow: HWND = parentHandle.toCPointer() ?: run {
            _logger.w { "CANNOT READ PARENT WINDOW" }
            return -1L
        }

        val new = CreateWindowExW(
            dwExStyle = WS_EX_TOOLWINDOW.toUInt() or WS_EX_NOACTIVATE.toUInt() or WS_EX_LAYERED.toUInt(),
            lpClassName = KLASS_NAME,
            lpWindowName = null,
            dwStyle = WS_POPUP,
            X = 0, Y = 0, nWidth = 200, nHeight = 60,
            hWndParent = parentWindow,
            hMenu = null,
            hInstance = instance,
            lpParam = null,
        ) ?: run {
            _logger.w { "CANNOT CREATE VIEW CreateWindowExW returned NULL" }
            return -1L
        }

        val backdropResult = WindowsUtility.applyWindows11Backdrop(new)
        if (backdropResult != S_OK) _logger.d { "CANNOT APPLY BACKDROP TO THE WINDOW" }

        val state = ToastStatePtr.allocate()
        SetWindowLongPtrW(hWnd = new, nIndex = GWLP_USERDATA, dwNewLong = state.rawAddress())

        val anim = WindowsAnimations().apply { initialize() }
        _logger.v { "ANIMATION STATE INITIALIZED" }

        val animStableRef = StableRef.create(anim)
        val pointed = animStableRef.asCPointer()
        state.animationRef = pointed.rawValue.toLong()

        SetLayeredWindowAttributes(hwnd = new, crKey = 0u, bAlpha = 0u, dwFlags = LWA_ALPHA.toUInt())
        _logger.i { "VIEW CREATED: hwindow=${new.rawValue}, StateAddress=${state.rawAddress()}, AnimRef=${state.animationRef}" }

        return new.rawValue.toLong()
    }

    actual override fun destroyView(viewHandle: Long) {
        _logger.v { "DESTROY VIEW CALLED  for handle=$viewHandle" }

        val hWindow: HWND = viewHandle.toCPointer() ?: run {
            _logger.w { "DESTROY VIEW FAILED: invalid pointer conversion for handle=$viewHandle" }
            return
        }
        if (IsWindow(hWnd = hWindow) == 0) {
            _logger.w { "DESTROY VIEW FAILED : HWND is not a valid window" }
            return
        }

        val killTimerResult = KillTimer(hWnd = hWindow, uIDEvent = TIMER_ID.toULong())
        _logger.d { "DESTROY VIEW SIDE-EFFECT : KillTimer IS_SUCCESSFULLY=${killTimerResult == 1}" }
        val destroyResult = DestroyWindow(hWnd = hWindow)
        _logger.i { "DESTROY VIEW SIDE-EFFECT : DestroyWindow SUCCESSFULLY:${destroyResult == 1}" }
    }

    actual override fun setBounds(viewHandle: Long, x: Int, y: Int, width: Int, height: Int) {
        _logger.v { "VIEW SIZE BOUNDS SET : x:$x y:$y width:$width height:$height" }

        val hWindow: HWND = viewHandle.toCPointer() ?: return
        val hideOp = ShowWindow(hWindow, SW_HIDE)
        _logger.d { "SET WINDOWS HIDDEN :${hideOp == 1}" }

        SetWindowPos(hWindow, null, x, y, width, height, SWP_NOZORDER.toUInt() or SWP_NOACTIVATE.toUInt())
    }

    actual override fun setCornerRadius(viewHandle: Long, radius: Float) {
        _logger.v { "VIEW CORNERS SET : radius:$radius" }

        val hWindow: HWND = viewHandle.toCPointer() ?: return
        WindowsUtility.applyCornerRadius(hWindow, radius)
    }

    actual override fun setBackgroundColor(viewHandle: Long, color: Int) {
        val hWindow: HWND = viewHandle.toCPointer() ?: return

        val address = GetWindowLongPtrW(hWnd = hWindow, nIndex = GWLP_USERDATA)
        val state = ToastStatePtr.at(address) ?: return

        val alpha = (color shr 24) and 0xFF
        val red = (color shr 16) and 0xff
        val green = (color shr 8) and 0xff
        val blue = color and 0xff

        val colorRef = (blue shl 16) or (green shl 8) or red

        state.backgroundColor = colorRef.toUInt()
        _logger.v { "SET WINDOWS COLOR ARGB :(a=$alpha,r=$red,g=$green,b=$blue)" }

        val hideOp = ShowWindow(hWindow, SW_HIDE)
        _logger.d { "SET WINDOWS HIDDEN :${hideOp == 1}" }

        RedrawWindow(hWindow, null, null, RDW_INVALIDATE.toUInt() or RDW_UPDATENOW.toUInt())
    }

    actual override fun show(viewHandle: Long, text: String, fadeInMs: Int, holdMs: Int, fadeOutMs: Int) {
        _logger.v { "SHOW TOAST INVOKED : text=$text" }

        val hWindow: HWND = viewHandle.toCPointer() ?: return
        SetWindowTextW(hWindow, text)

        val address = GetWindowLongPtrW(hWnd = hWindow, nIndex = GWLP_USERDATA)
        val state = ToastStatePtr.at(address)?.apply {
            this.fadeInMs = fadeInMs.toUInt()
            this.holdMs = holdMs.toUInt()
            this.fadeOutMs = fadeOutMs.toUInt()
            this.elapsedMs = 0.toUInt()
            this.alpha = 0.toUByte()
            this.phase = Phase.FadeIn
        } ?: return

        val animRef = state.animationRef.toCPointer<COpaquePointerVar>()?.asStableRef<WindowsAnimations>()
        val anim = animRef?.get() ?: return
        anim.animateTo(targetValue = 1.0, durationSeconds = fadeInMs.toDouble() / 1000.0)
        // Force an immediate update so the first timer tick has valid data
        anim.getCurrentAlpha()

        SetLayeredWindowAttributes(hwnd = hWindow, crKey = 0u, bAlpha = 0u, dwFlags = LWA_ALPHA.toUInt())
        ShowWindow(hWnd = hWindow, nCmdShow = SW_SHOWNOACTIVATE)
        RedrawWindow(hWindow, null, null, RDW_INVALIDATE.toUInt() or RDW_UPDATENOW.toUInt())
        SetTimer(
            hWnd = hWindow,
            nIDEvent = TIMER_ID.toULong(),
            uElapse = TIMER_INTERVAL_MS.toUInt(),
            lpTimerFunc = null,
        )
    }


    private fun ensureClassRegistered(instance: HINSTANCE) = memScoped {
        if (_isRegistered.load()) {
            _logger.w { "ENSURE CLASS REGISTERED SKIPPED : class already registered" }
            return@memScoped
        }

        val wc = alloc<WNDCLASSEXW>().apply {
            cbSize = sizeOf<WNDCLASSEXW>().toUInt()
            lpfnWndProc = windowsProc
            hCursor = LoadCursor?.invoke(null, IDC_ARROW)
            hInstance = instance
            style = CS_DROPSHADOW.toUInt()
            lpszClassName = KLASS_NAME.wcstr.ptr
            hbrBackground = null
        }
        val result = RegisterClassExW(wc.ptr)
        _isRegistered.compareAndSet(expectedValue = false, newValue = true)
        _logger.d { "ENSURE CLASS REGISTERED  STATUS DONE CODE:${result.toLong()}" }
    }


    companion object {
        private const val TIMER_ID = 1

        private const val TIMER_INTERVAL_MS = 16
        private const val KLASS_NAME = "NATIVE_TOAST"

        private val _isRegistered = AtomicBoolean(false)

        private val _logger = Logger(
            tag = "NATIVE_WINDOW",
            config = object : LoggerConfig {
                override val minSeverity: Severity = Severity.Debug
                override val logWriterList: List<LogWriter> = listOf(WindowsLogWriter())
            },
        )

        private fun toColorRef(argb: UInt): COLORREF {
            val r = (argb shr 16) and 0xFFu
            val g = (argb shr 8) and 0xFFu
            val b = argb and 0xFFu
            return (b shl 16) or (g shl 8) or r
        }

        private val windowsProc = staticCFunction { hWindow: HWND?, msg: UINT, wParam: WPARAM, lParam: LPARAM ->
            when (msg.toInt()) {
                WM_ERASEBKGND -> memScoped {
                    _logger.d { "WINDOW BACKGROUND ERASED " }
                    val hdc = wParam.toLong().toCPointer<HDC__>() ?: return@staticCFunction 1L
                    val rect = alloc<RECT>()
                    GetClientRect(hWindow, rect.ptr)

                    val ptr = GetWindowLongPtrW(hWindow, GWLP_USERDATA)
                    val state = ToastStatePtr.at(ptr)
                    val rawColor = state?.backgroundColor ?: 0x000000u
                    val brush = CreateSolidBrush(toColorRef(rawColor))
                    FillRect(hdc, rect.ptr, brush)
                    DeleteObject(brush)
                    1L
                }

                WM_PAINT -> memScoped {
                    _logger.d { "WINDOW PAINTED" }
                    val ps = alloc<PAINTSTRUCT>()
                    val hdc = BeginPaint(hWindow, ps.ptr)
                    val rect = alloc<RECT>()
                    GetClientRect(hWindow, rect.ptr)

                    val hFont = WindowsUtility.createDefaultFont()
                    val oldFont = SelectObject(hdc, hFont)
                    SetTextColor(hdc, 0x00FFFFFFu)
                    SetBkMode(hdc, TRANSPARENT)

                    val buf = allocArray<UShortVar>(256)
                    GetWindowTextW(hWindow, buf, 256);
                    DrawTextW(
                        hdc = hdc,
                        lpchText = buf.toKStringFromUtf16(),
                        cchText = -1,
                        lprc = rect.ptr,
                        format = DT_CENTER.toUInt() or DT_VCENTER.toUInt() or DT_SINGLELINE.toUInt(),
                    )
                    if (oldFont != null) SelectObject(hdc, oldFont)
                    DeleteObject(hFont)
                    EndPaint(hWindow, ps.ptr)
                    0L
                }

                WM_TIMER -> {
                    _logger.v { "TIMER EXPIRED" }
                    val ptr = GetWindowLongPtrW(hWindow, GWLP_USERDATA)
                    val state = ToastStatePtr.at(ptr) ?: return@staticCFunction 0L

                    val animRef = state.animationRef.toCPointer<COpaquePointerVar>()?.asStableRef<WindowsAnimations>()
                    val anim = animRef?.get() ?: return@staticCFunction 0L

                    if (state.phase == Phase.Idle) {
                        _logger.v { "IDLE PHASE HIDING THE WINDOW" }
                        KillTimer(hWindow, TIMER_ID.toULong())
                        ShowWindow(hWindow, SW_HIDE)
                        return@staticCFunction 0L
                    }

                    val rawNormalizedAlpha = anim.getCurrentAlpha().coerceIn(0.0, 1.0)
                    val win32ByteAlpha = (rawNormalizedAlpha * 255.0).toInt().toUByte()
                    state.alpha = win32ByteAlpha
                    SetLayeredWindowAttributes(hWindow, 0u, state.alpha, LWA_ALPHA.toUInt())

                    _logger.v { "CURRENT PHASE :${state.phase} :$rawNormalizedAlpha" }
                    when (state.phase) {
                        Phase.FadeIn -> if (rawNormalizedAlpha >= 0.95) {
                            _logger.v { "FADE-IN COMPLETE -> ENTERING HOLD" }
                            state.phase = Phase.Hold
                            state.elapsedMs = 0u
                        }

                        Phase.Hold -> state.advance(interval = TIMER_INTERVAL_MS) {
                            anim.animateTo(targetValue = 0.0, durationSeconds = state.fadeOutMs.toDouble() / 1000.0)
                            _logger.v { "HOLD COMPLETE -> ENTERING  FADEOUT" }
                        }
                        // Spring target returned to 0.0 opacity
                        Phase.FadeOut -> if (rawNormalizedAlpha <= 0.01) {
                            state.phase = Phase.Idle
                            SetLayeredWindowAttributes(hWindow, 0u, 0u, LWA_ALPHA.toUInt())
                            KillTimer(hWindow, TIMER_ID.toULong())
                            ShowWindow(hWindow, SW_HIDE)
                        }

                        else -> return@staticCFunction 0L
                    }
                    0L
                }

                WM_NCDESTROY -> {
                    _logger.d { "WINDOW NON_CLIENT DESTROY" }
                    val address = GetWindowLongPtrW(hWindow, GWLP_USERDATA)
                    val state = ToastStatePtr.at(address) ?: return@staticCFunction 0L

                    val animRef = state.animationRef.toCPointer<COpaquePointerVar>()?.asStableRef<WindowsAnimations>()
                    val animation = animRef?.get()
                    animation?.onDestroy()
                    _logger.v { "ANIMATION STATE DESTROYED" }
                    animRef?.dispose()

                    state.free()
                    0L
                }

                else -> DefWindowProcW(hWindow, msg, wParam, lParam)
            }
        }
    }
}
