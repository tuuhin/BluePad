package com.sam.bluepad.domain.platform

import com.sam.bluepad.domain.models.DevicePlatformOS

/**
 * Interface for reading hardware and system platform details.
 */
interface IPlatformInfoReader {

    /**
     * Gets the high-level operating system type as an enum (e.g., Android, iOS, Windows).
     * This is a quick synchronous check for the platform's OS family.
     */
    val platformOS: DevicePlatformOS

    /**
     * Reads detailed hardware and platform device information asynchronously.
     * @return A [Result] containing the [PlatformDeviceInfo] on success,
     * or a failure exception if reading device details fails.
     */
    suspend fun readPlatform(): Result<PlatformDeviceInfo>
}
