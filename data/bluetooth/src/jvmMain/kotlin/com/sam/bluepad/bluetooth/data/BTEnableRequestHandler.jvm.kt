package com.sam.bluepad.bluetooth.data

import co.touchlab.kermit.Logger
import com.sam.bluepad.bluetooth.data.ext.description
import com.sam.bluepad.bluetooth.exceptions.BluetoothEnableFailedException
import com.sam.bluepad.common.utils.ICoroutineDispatchersProvider
import com.sam.bluepad.domain.bluetooth.IBTEnableRequestHandler
import com.sam.bt_common.models.BTJVMEnableResult
import com.sam.bt_common.platform.KotlinNativeException
import com.sam.bt_common.platform.PlatformBTInfoProvider
import com.sam.bt_common.requestBTEnableAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

private const val TAG = "BT_ENABLE_REQUEST_HANDLER"

@Single(binds = [IBTEnableRequestHandler::class])
internal actual class BTEnableRequestHandler(
    private val dispatchers: ICoroutineDispatchersProvider
) : IBTEnableRequestHandler {


    private val mLock = Mutex()

    actual override val canOpenSettingsToActivateBT: Boolean
        get() = PlatformBTInfoProvider().use { it.canRequestOpenSettings }

    actual override val canRequestBTActive: Boolean
        get() = PlatformBTInfoProvider().use { it.canActivateBTFromApp }

    actual override suspend fun requestActive(): Result<Unit> {
        return runCatching {
            if (mLock.isLocked) {
                Logger.w(tag = TAG) { "REQUEST ALREADY BEING MADE PLEASE WAIT" }
                return@runCatching
            }
            val status = mLock.withLock { PlatformBTInfoProvider.requestBTEnableAsync() }
            when (status) {
                BTJVMEnableResult.REQUEST_ACCEPTED, BTJVMEnableResult.REQUEST_NOT_NEEDED -> return@runCatching
                else -> throw BluetoothEnableFailedException(status.description)
            }
        }
    }

    actual override suspend fun onOpenSettings() {
        withContext(dispatchers.io) {
            try {
                PlatformBTInfoProvider().use { it.openBTSettings() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: KotlinNativeException) {
                Logger.e(tag = TAG, throwable = e) { "FAILED TO OPEN BLUETOOTH SETTINGS" }
            }
        }
    }
}
