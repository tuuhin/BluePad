package com.sam.bluepad.common.utils

import com.sam.bluepad.common.models.PlatformOS

interface IPlatformDataProvider {


    val platformOS: PlatformOS
    val platformOSVersion: String
}
