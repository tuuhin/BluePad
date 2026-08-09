package com.sam.bluepad.common.utils

import com.sam.bluepad.common.models.PlatformOS

expect class PlatformDataProvider : IPlatformDataProvider {


    override val platformOSVersion: String
    override val platformOS: PlatformOS
}
