package com.sam.bluepad.utility

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.sam.bluepad.platform.common_utils.NativePlatformFontProviderImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ReadDeviceFontTest {

    @Test
    fun `read device font without crashing`() = runTest {
        val provider = NativePlatformFontProviderImpl()
        provider.use {
            val font = it.readFontFamily()
            assertThat(font).isNotNull()
                .isNotEmpty()
        }
    }
}
