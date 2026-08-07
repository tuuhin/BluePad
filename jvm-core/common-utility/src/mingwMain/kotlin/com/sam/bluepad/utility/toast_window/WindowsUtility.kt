package com.sam.bluepad.utility.toast_window

import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.windows.ANTIALIASED_QUALITY
import platform.windows.CLIP_DEFAULT_PRECIS
import platform.windows.CreateFontW
import platform.windows.CreateRoundRectRgn
import platform.windows.DEFAULT_CHARSET
import platform.windows.DwmSetWindowAttribute
import platform.windows.FF_SWISS
import platform.windows.FW_NORMAL
import platform.windows.GetClientRect
import platform.windows.HFONT
import platform.windows.HWND
import platform.windows.OUT_DEFAULT_PRECIS
import platform.windows.RECT
import platform.windows.S_OK
import platform.windows.SetWindowRgn
import platform.windows.TRUE
import platform.windows.VARIABLE_PITCH
import kotlin.math.roundToInt

internal object WindowsUtility {

    private const val SYSTEM_BACKDROP_TYPE = 38
    private const val AUTO = 0
    private const val NONE = 1
    private const val MAIN_WINDOW = 2 // MICA
    private const val TRANSIENT_WINDOW = 3 // Acrylic
    private const val TABBED_WINDOW = 4 // MICA

    private const val WINDOW_CORNER_PREFERENCE = 33
    private const val DEFAULT = 0
    private const val DO_NOT_ROUND = 1
    private const val ROUND = 2       // Standard Windows 11 rounding (~8-12px)
    private const val ROUNDSMALL = 3

    fun applyWindows11Backdrop(hWnd: HWND, backdropType: Int = TRANSIENT_WINDOW) = memScoped {
        val backdrop = alloc<IntVar>().apply { value = backdropType }

        DwmSetWindowAttribute(
            hwnd = hWnd,
            dwAttribute = SYSTEM_BACKDROP_TYPE.toUInt(),
            pvAttribute = backdrop.ptr,
            cbAttribute = sizeOf<IntVar>().toUInt(),
        )
    }

    fun applyCornerRadius(hWnd: HWND, radius: Float) = memScoped {
        val prefs = alloc<IntVar>().apply { value = if (radius > 6) ROUND else ROUNDSMALL }

        val result = DwmSetWindowAttribute(
            hwnd = hWnd,
            dwAttribute = WINDOW_CORNER_PREFERENCE.toUInt(),
            pvAttribute = prefs.ptr,
            cbAttribute = sizeOf<IntVar>().toUInt(),
        )
        if (result == S_OK) return@memScoped
        if (radius <= 0f) return@memScoped

        val rect = alloc<RECT>()
        GetClientRect(hWnd, rect.ptr)

        val width = rect.right - rect.left
        val height = rect.bottom - rect.top

        val hRgn = CreateRoundRectRgn(
            0, 0,
            width,
            height,
            (radius * 2).roundToInt(),
            (radius * 2).roundToInt(),
        )
        SetWindowRgn(hWnd = hWnd, hRgn = hRgn, bRedraw = TRUE)
    }

    fun createDefaultFont(sizeSp: Int = 14): HFONT? {
        val height = -((sizeSp * 96) / 72)
        return CreateFontW(
            cHeight = height,
            cWidth = 0,
            cEscapement = 0,
            cOrientation = 0,
            cWeight = FW_NORMAL,
            bItalic = 0u,
            bUnderline = 0u,
            bStrikeOut = 0u,
            iCharSet = DEFAULT_CHARSET.toUInt(),
            iOutPrecision = OUT_DEFAULT_PRECIS.toUInt(),
            iClipPrecision = CLIP_DEFAULT_PRECIS.toUInt(),
            iQuality = ANTIALIASED_QUALITY.toUInt(),
            iPitchAndFamily = VARIABLE_PITCH.toUInt() or FF_SWISS.toUInt(),
            pszFaceName = "Segoe UI",
        )
    }
}
