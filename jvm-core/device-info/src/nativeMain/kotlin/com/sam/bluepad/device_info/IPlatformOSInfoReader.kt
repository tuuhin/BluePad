package com.sam.bluepad.device_info

interface IPlatformOSInfoReader {
    val osName: String
    val osVersion: String
    val buildNumber: String
    val arch: String
}
