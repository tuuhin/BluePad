package com.sam.bluepad.data.bluetooth

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.bluetooth.BTEnableRequestProvider
import com.sam.bt_common.models.BTJVMEnableResult
import com.sam.bt_common.platform.PlatformBTInfoProvider
import com.sam.bt_common.requestBTEnableAsync
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "BluetoothEnableRequest"

actual class BTEnableRequestProviderImpl(
    private val dispatchers: PlatformDispatcherProvider
) : BTEnableRequestProvider {

    private val _lock = Mutex()

    override val canOpenSettingsToActivateBT: Boolean
        get() = PlatformBTInfoProvider().use { it.canRequestOpenSettings }

    override val canRequestBTActive: Boolean
        get() = PlatformBTInfoProvider().use { it.canActivateBTFromApp }

    override suspend fun requestActive(): Result<Unit> {
        return runCatching {
            if (_lock.isLocked) {
                Logger.w(tag = TAG) { "REQUEST ALREADY BEING MADE PLEASE WAIT" }
            }
            val status = _lock.withLock { PlatformBTInfoProvider.requestBTEnableAsync() }

            when (status) {
                BTJVMEnableResult.REQUEST_ACCEPTED, BTJVMEnableResult.REQUEST_NOT_NEEDED -> return@runCatching
                BTJVMEnableResult.REQUEST_DENIED_PRIVACY_ISSUES -> throw IllegalStateException("Cannot request access privacy issues")
                BTJVMEnableResult.REQUEST_DENIED_BY_SYSTEM -> throw IllegalStateException("Request denied by system")
                BTJVMEnableResult.REQUEST_DENIED_BY_USER -> throw IllegalStateException("Request denied by user")
                BTJVMEnableResult.REQUEST_DENIED_CANNOT_FIND_ADAPTER -> throw IllegalStateException("Cannot find a bluetooth adapter")
                BTJVMEnableResult.REQUEST_DENIED_UNKNOWN -> throw IllegalStateException("Request state cannot be determined")
                BTJVMEnableResult.REQUEST_OPTION_NOT_FOUND -> throw IllegalStateException("Open not found, this state is kind a impossible")
            }
        }
    }

    override suspend fun onOpenSettings() {
        withContext(dispatchers.io) {
            try {
                PlatformBTInfoProvider().use { it.openBTSettings() }
            } catch (e: Exception) {
                Logger.e(tag = TAG, throwable = e) { "FAILED TO OPEN BLUETOOTH SETTINGS" }
            }
        }
    }
}
