package com.sam.bluepad.domain.bluetooth

/**
 * Manages logic and platform interactions required
 * to check Bluetooth availability and request state changes.
 */
interface BTEnableRequestProvider {

    /**
     * Indicates whether the app can programmatically request the user to turn on Bluetooth
     * @return `true` if the device supports Bluetooth and platform can activate it
     */
    val canRequestBTActive: Boolean

    /**
     * Indicates whether the app can direct the user to the device Settings screen
     * to manually enable Bluetooth.
     * Useful as a fallback when programmatic activation is unavailable or rejected.
     */
    val canOpenSettingsToActivateBT: Boolean

    /**
     * Prompts the user to enable Bluetooth programmatically.
     *
     * @return A [Result] containing [Unit] on successful activation,
     * or a failure containing the corresponding [Throwable] if the request failed or was denied.
     */
    suspend fun requestActive(): Result<Unit>

    /**
     * Navigates the user to the system Bluetooth Settings page
     */
    suspend fun onOpenSettings()
}
