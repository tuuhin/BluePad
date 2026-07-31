package com.sam.bluepad.utility.toast_window

import co.touchlab.kermit.Logger
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.wcstr
import platform.windows.BeginPaint
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
import platform.windows.WS_EX_NOACTIVATE
import platform.windows.WS_EX_TOOLWINDOW
import platform.windows.WS_POPUP
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
actual class NativeToastViewImpl : INativeToastView {

    private val _logger = Logger.withTag("NATIVE_WINDOWS_CONTAINER")

    private val _isRegistered = AtomicBoolean(false)

    actual override fun createView(parentHandle: Long): Long {

        _logger.d { "CREATE VIEW CALLED" }

        val instance = GetModuleHandleW(lpModuleName = null) ?: run {
            _logger.d { "CANNOT CREATE A VIEW  GetModuleHandleW returned null" }
            return -1L
        }
        ensureClassRegistered(instance)

        val parentWindow: HWND = parentHandle.toCPointer() ?: run {
            _logger.d { "CANNOT READ PARENT WINDOW" }
            return -1L
        }

        val new = CreateWindowExW(
            dwExStyle = WS_EX_TOOLWINDOW.toUInt() or WS_EX_NOACTIVATE.toUInt(),
            lpClassName = KLASS_NAME,
            lpWindowName = null,
            dwStyle = WS_POPUP,
            X = 0, Y = 0, nWidth = 200, nHeight = 60,
            hWndParent = parentWindow,
            hMenu = null,
            hInstance = instance,
            lpParam = null,
        ) ?: run {
            _logger.d { "CANNOT CREATE VIEW CreateWindowExW returned NULL" }
            return -1L
        }

        val backdropResult = WindowsUtility.applyWindows11Backdrop(new)
        if (backdropResult != S_OK) _logger.d { "CANNOT APPLY BACKDROP TO THE WINDOW" }


        val state = ToastStatePtr.allocate()
        SetWindowLongPtrW(hWnd = new, nIndex = GWLP_USERDATA, dwNewLong = state.rawAddress())
        SetLayeredWindowAttributes(hwnd = new, crKey = 0u, bAlpha = 0u, dwFlags = LWA_ALPHA.toUInt())

        _logger.d { "VIEW CREATED: hWnd=${new.rawValue}, StateAddress=${state.rawAddress()}" }
        return new.rawValue.toLong()
    }

    actual override fun destroyView(viewHandle: Long) {
        _logger.d { "DESTROY VIEW CALLED  for handle=$viewHandle" }

        val hWindow: HWND = viewHandle.toCPointer() ?: run {
            _logger.d { "DESTROY VIEW FAILED: invalid pointer conversion for handle=$viewHandle" }
            return
        }
        if (IsWindow(hWnd = hWindow) == 0) {
            _logger.d { "DESTROY VIEW FAILED : HWND is not a valid window" }
            return
        }
        val killTimerResult = KillTimer(hWnd = hWindow, uIDEvent = TIMER_ID.toULong())
        _logger.d { "DESTROY VIEW SIDE-EFFECT : KillTimer result=$killTimerResult" }
        val destroyResult = DestroyWindow(hWnd = hWindow)
        _logger.d { "DESTROY VIEW SIDE-EFFECT : DestroyWindow result=$destroyResult" }
    }

    actual override fun setBounds(viewHandle: Long, x: Int, y: Int, width: Int, height: Int) {
        val hWindow: HWND = viewHandle.toCPointer() ?: return
        SetWindowPos(
            hWnd = hWindow,
            hWndInsertAfter = null,
            X = x,
            Y = y,
            cx = width,
            cy = height,
            uFlags = SWP_NOZORDER.toUInt() or SWP_NOACTIVATE.toUInt(),
        )
        _logger.d { "VIEW SIZE BOUNDS SET : x:$x y:$y width:$width height:$height" }
    }

    actual override fun setCornerRadius(viewHandle: Long, radius: Float) {
        val hWindow: HWND = viewHandle.toCPointer() ?: return
        WindowsUtility.applyCornerRadius(hWindow, radius)
        _logger.d { "VIEW CORNERS SET : radius:$radius" }
    }

    actual override fun setBackgroundColor(viewHandle: Long, color: Int) {
        val hWindow: HWND = viewHandle.toCPointer() ?: return

        val address = GetWindowLongPtrW(hWnd = hWindow, nIndex = GWLP_USERDATA)
        val state = ToastStatePtr.at(address) ?: return

        val red = (color shr 16) and 0xff
        val green = (color shr 8) and 0xff
        val blue = color and 0xff
        val colorRef = (blue shl 16) or (green shl 8) or red

        state.backgroundColor = colorRef.toUInt()

        RedrawWindow(
            hWnd = hWindow,
            lprcUpdate = null,
            hrgnUpdate = null,
            flags = RDW_INVALIDATE.toUInt() or RDW_UPDATENOW.toUInt(),
        )
        _logger.d { "VIEW COLORS SET: COLOR ${color.toByte()}" }
    }

    actual override fun show(viewHandle: Long, text: String, fadeInMs: Int, holdMs: Int, fadeOutMs: Int) {
        val hWindow: HWND = viewHandle.toCPointer() ?: return

        SetWindowTextW(hWindow, text)

        val address = GetWindowLongPtrW(hWnd = hWindow, nIndex = GWLP_USERDATA)
        val state = ToastStatePtr.at(address)?.apply {
            this.fadeInMs = fadeInMs
            this.holdMs = holdMs
            this.fadeOutMs = fadeOutMs
            this.elapsedMs = 0
            this.alpha = 0
            this.phase = Phase.FadeIn
        } ?: return

        SetWindowLongPtrW(hWindow, GWLP_USERDATA, state.rawAddress())
        SetLayeredWindowAttributes(hwnd = hWindow, crKey = 0u, bAlpha = 0u, dwFlags = LWA_ALPHA.toUInt())
        ShowWindow(hWnd = hWindow, nCmdShow = SW_SHOWNOACTIVATE)
        SetTimer(
            hWnd = hWindow,
            nIDEvent = TIMER_ID.toULong(),
            uElapse = TIMER_INTERVAL_MS.toUInt(),
            lpTimerFunc = null,
        )
    }


    private fun ensureClassRegistered(instance: HINSTANCE) = memScoped {
        if (_isRegistered.load()) {
            _logger.d { "ENSURE CLASS REGISTERED SKIPPED : class already registered" }
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
        private const val KLASS_NAME = "NATIVE_TOAST"
        private const val TIMER_INTERVAL_MS = 16

        private val windowsProc = staticCFunction { hWindow: HWND?, msg: UINT, wParam: WPARAM, lParam: LPARAM ->
            when (msg.toInt()) {
                WM_ERASEBKGND -> memScoped {
                    val hdc = wParam.toLong().toCPointer<HDC__>() ?: return@memScoped 1L
                    val rect = alloc<RECT>()
                    GetClientRect(hWindow, rect.ptr)

                    val ptr = GetWindowLongPtrW(hWindow, GWLP_USERDATA)
                    val state = ToastStatePtr.at(ptr)
                    val colorRef = state?.backgroundColor ?: 0x000000u

                    val brush = CreateSolidBrush(colorRef)
                    FillRect(hdc, rect.ptr, brush)
                    DeleteObject(brush)
                    1L
                }

                WM_PAINT -> memScoped {
                    val ps = alloc<PAINTSTRUCT>()
                    val hdc = BeginPaint(hWindow, ps.ptr)
                    val rect = alloc<RECT>()
                    GetClientRect(hWindow, rect.ptr)

                    val hFont = WindowsUtility.createDefaultFont()
                    val oldFont = SelectObject(hdc, hFont)
                    SetTextColor(hdc, 0xffffffu)
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
                    val ptr = GetWindowLongPtrW(hWindow, GWLP_USERDATA)
                    val state = ToastStatePtr.at(ptr) ?: return@staticCFunction 0L
                    val stillRunning = state.advance()
                    SetLayeredWindowAttributes(hWindow, 0u, state.alpha.toUByte(), LWA_ALPHA.toUInt())
                    if (stillRunning) return@staticCFunction 0L
                    ShowWindow(hWindow, SW_HIDE)
                    KillTimer(hWindow, TIMER_ID.toULong())
                    0L
                }

                WM_NCDESTROY -> {
                    val address = GetWindowLongPtrW(hWindow, GWLP_USERDATA)
                    ToastStatePtr.at(address)?.free()
                    0L
                }

                else -> DefWindowProcW(hWindow, msg, wParam, lParam)
            }
        }
    }
}
