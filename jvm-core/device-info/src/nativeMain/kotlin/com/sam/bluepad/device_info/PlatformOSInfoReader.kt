package com.sam.bluepad.device_info

expect class PlatformOSInfoReader : IPlatformOSInfoReader {
    override val arch: String
    override val buildNumber: String
    override val osName: String
    override val osVersion: String
}
