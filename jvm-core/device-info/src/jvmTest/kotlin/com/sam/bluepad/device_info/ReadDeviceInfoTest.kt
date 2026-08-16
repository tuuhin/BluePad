package com.sam.bluepad.device_info

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.sam.bluepad.com.sam.bluepad.device_info.IJvmDeviceInfoReader
import com.sam.bluepad.com.sam.bluepad.device_info.JVMDeviceInfoReader
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ReadDeviceInfoTest {

    lateinit var reader: IJvmDeviceInfoReader

    @BeforeTest
    fun setup() {
        reader = JVMDeviceInfoReader()
    }

    @Test
    fun `read device info and check all the fields are not null`() = runTest {
        val device = reader.readDevice()

        println(device)

        assertThat(device.macAddress)
            .isNotNull()
            .isNotEmpty()

        assertThat(device.bluetoothAdapter)
            .isNotNull()
            .isNotEmpty()
    }
}
