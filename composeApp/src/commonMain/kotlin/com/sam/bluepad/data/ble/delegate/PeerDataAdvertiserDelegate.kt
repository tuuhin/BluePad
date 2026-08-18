package com.sam.bluepad.data.ble.delegate

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.exceptions.BLEAdvertiserException
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.platform.IPlatformInfoReader
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class PeerDataAdvertiserDelegate(
    val protoBuf: ProtoBuf,
    val randomGenerator: RandomGenerator,
    val platformInfoProvider: IPlatformInfoReader,
    val dispatchers: PlatformDispatcherProvider,
) {

    suspend fun handleDeviceReadRequest(currentDeviceInfo: LocalDeviceInfoModel? = null): Result<ByteArray> {
        return runCatching {
            if (currentDeviceInfo == null) throw BLEAdvertiserException.LocalIdentityMissingException()

            withContext(dispatchers.io) {
                val nonce = randomGenerator.generateRandomBytes(size = NONCE_SIZE)
                val peerData = BLEPeerData(
                    deviceId = currentDeviceInfo.deviceId,
                    deviceOs = platformInfoProvider.platformOS,
                    deviceName = currentDeviceInfo.name,
                    nonce = nonce.decodeToString(),
                )
                Logger.d(tag = TAG) { "OUTGOING PEER DEVICE DATA" }
                protoBuf.encodeToByteArray<BLEPeerData>(peerData)
            }
        }
    }

    suspend fun handleDeviceWriteRequest(value: ByteArray): Result<BLEPeerData> = runCatching {
        Logger.d(tag = TAG) { "INCOMING PEER DEVICE DATA" }
        withContext(dispatchers.io) {
            protoBuf.decodeFromByteArray<BLEPeerData>(value)
        }
    }

    companion object {
        const val TAG = "PEER_DATA_ADVERTISER_DELEGATE"
        const val NONCE_SIZE = 12
    }
}
