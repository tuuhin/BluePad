package com.sam.bluepad.bluetooth.data

import com.sam.bluepad.domain.bluetooth.IBTEnableRequestHandler

internal expect class BTEnableRequestHandler : IBTEnableRequestHandler {


    override val canOpenSettingsToActivateBT: Boolean
    override val canRequestBTActive: Boolean

    override suspend fun onOpenSettings()

    override suspend fun requestActive(): Result<Unit>
}
