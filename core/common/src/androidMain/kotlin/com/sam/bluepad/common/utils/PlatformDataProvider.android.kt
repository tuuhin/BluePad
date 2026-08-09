package com.sam.bluepad.common.utils

import android.os.Build
import com.sam.bluepad.common.models.PlatformOS
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformDataProvider::class])
actual class PlatformDataProvider : IPlatformDataProvider {


    actual override val platformOSVersion: String
        get() = "${Build.VERSION.SDK_INT}"

    actual override val platformOS: PlatformOS
        get() = PlatformOS.ANDROID
}
