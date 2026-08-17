package com.sam.bluepad.device_info

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.sam.bluepad.com.sam.bluepad.device_info.IJvmDeviceInfoReader
import com.sam.bluepad.com.sam.bluepad.device_info.IJvmPlatformOSInfoReader
import com.sam.bluepad.com.sam.bluepad.device_info.JVMDeviceInfoReader
import com.sam.bluepad.com.sam.bluepad.device_info.JVMPlatformInfoReader
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ReadDeviceInfoTest {

    lateinit var reader: IJvmDeviceInfoReader
    lateinit var deviceReader: IJvmPlatformOSInfoReader

    @BeforeTest
    fun setup() {
        reader = JVMDeviceInfoReader()
        deviceReader = JVMPlatformInfoReader()
    }

    @Test
    fun `read device bluetooth adapter info and check all the fields are not null`() = runTest {
        val adapter = reader.readDevice()

        assertThat(adapter.macAddress).isNotNull().isNotEmpty()
        assertThat(adapter.bluetoothAdapter).isNotNull().isNotEmpty()
        assertThat(adapter.bluetoothVendor).isNotNull().isNotEmpty()
        println(adapter)
    }

    @Test
    fun `read device platform info and check if the fields are not null`() = runTest {
        val device = deviceReader.invoke()

        assertThat(device.osName).isNotEmpty()
        assertThat(device.osVersion).isNotEmpty()
        assertThat(device.arch).isNotEmpty()
        assertThat(device.buildNumber).isNotEmpty()

    }
}
