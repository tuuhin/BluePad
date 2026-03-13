package com.sam.bluepad.data.ble.callbacks

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.utils.btDescriptorsNotificationOrIndicationEnabled
import com.sam.bluepad.data.ble.utils.hasIndication
import com.sam.bluepad.data.sync.dto.BLEHandshakeFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.data.sync.dto.BLESyncFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncHandshakeData
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

class ServerConnectionDelegate(
    private val protoBuf: ProtoBuf,
    private val randomGenerator: RandomGenerator,
    private val platformInfoProvider: PlatformInfoProvider,
    private val encoder: BytesEncoder,
    private val inPayloadManager: InPayloadManager,
    private val outPayloadManager: OutPayloadManager,
) {

    private val _localNonceMap = ConcurrentHashMap<String, String>()
    private val _cccDescriptorMap = ConcurrentHashMap<String, Boolean>()

    var onCharacteristicsChanged: GATTNotifyCharacteristicsChanged? = null

    fun handleDeviceReadRequest(currentDeviceInfo: LocalDeviceInfoModel? = null): Result<ByteArray> {
        if (currentDeviceInfo == null) {
            Logger.w(TAG) { "NO DEVICE INFO PROVIDED" }
            return Result.failure(LocalDeviceInfoMissing())
        }

        return try {
            val nonce = randomGenerator.generateRandomBytes(size = NONCE_SIZE)
            val peerData = BLEPeerData(
                deviceId = currentDeviceInfo.deviceId,
                deviceOs = platformInfoProvider.platformOS,
                deviceName = currentDeviceInfo.name,
                nonce = nonce.decodeToString(),
            )
            val bytes = protoBuf.encodeToByteArray<BLEPeerData>(peerData)
            Result.success(bytes)
        } catch (e: Exception) {
            Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
            Result.failure(e)
        }
    }

    fun handleDeviceWriteRequest(value: ByteArray): Result<BLEPeerData> {
        return try {
            val peerData = protoBuf.decodeFromByteArray<BLEPeerData>(value)
            Result.success(peerData)
        } catch (e: Exception) {
            Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
            Result.failure(e)
        }
    }

    fun handleProximityReadRequest(
        device: BluetoothDevice,
        currentDeviceInfo: LocalDeviceInfoModel? = null,
    ): Result<ByteArray> {
        if (currentDeviceInfo == null) {
            Logger.w(TAG) { "NO DEVICE INFO PROVIDED" }
            return Result.failure(LocalDeviceInfoMissing())
        }

        return try {
            val nonce = randomGenerator.generateRandomBytes(NONCE_SIZE).let { nonceBytes ->
                encoder.encodeBytes(nonceBytes)
                    .apply { _localNonceMap[device.address] = this }
            }
            val data = BLESyncHandshakeData.AdvertiseDeviceData(
                deviceId = currentDeviceInfo.deviceId,
                nonce = nonce,
                allowSync = true,
            )
            val bytes =
                protoBuf.encodeToByteArray<BLESyncHandshakeData.AdvertiseDeviceData>(data)
            Logger.d(TAG) { "RESPONDING WITH DEVICE DATA" }
            Result.success(bytes)
        } catch (e: Exception) {
            Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
            Result.failure(e)
        }
    }

    suspend fun handleProximityWriteRequest(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        savedDevices: suspend (Uuid) -> Result<ExternalDeviceModel>,
        currentDeviceInfo: LocalDeviceInfoModel? = null,
    ): Result<ExternalDeviceModel> {
        return try {
            val response = protoBuf.decodeFromByteArray<BLESyncHandshakeData.AdvertiseResponseData>(value)

            val savedNonce = _localNonceMap[device.address]
            val externalDevice = savedDevices(response.senderID).getOrNull()

            val failedReason = when {
                savedNonce == null -> BLEHandshakeFailedReason.INVALID_INCOMING_DATA
                savedNonce != response.nonce -> BLEHandshakeFailedReason.TAMPERED_DATA
                response.receiverID != currentDeviceInfo?.deviceId -> BLEHandshakeFailedReason.INVALID_RECEIVER
                externalDevice == null -> BLEHandshakeFailedReason.UNKNOWN_SENDER
                else -> null
            }

            val data = failedReason?.let { BLESyncHandshakeData.HandshakeACKFailed(it) }
                ?: BLESyncHandshakeData.HandshakeACKSuccess(response.nonce)

            val bytes = protoBuf.encodeToByteArray<BLESyncHandshakeData>(data)
            onCharacteristicsChanged?.invoke(device, characteristic, bytes)

            if (externalDevice == null) return Result.failure(MissingSavedDeviceException())
            val message = when (data) {
                is BLESyncHandshakeData.HandshakeACKSuccess -> "ACK SUCCESS"
                is BLESyncHandshakeData.HandshakeACKFailed -> "ACK FAILED : REASON :${data.reason}"
                else -> ""
            }
            Logger.d(TAG) { "SENDING ACK DATA ON CHARACTERISTICS:${characteristic.uuid} $message" }
            Result.success(externalDevice)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
            Result.failure(e)
        }
    }

    suspend fun handleSyncDataWriteRequest(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Result<Unit> {
        return try {
            // read the response
            when (val response = protoBuf.decodeFromByteArray<BLESyncSession>(value)) {
                BLESyncSession.SyncSessionStart -> sendSessionStartACK(device, characteristic)
                is BLESyncSession.BLESyncDataPacket -> manageSyncSessionDataPacket(device, characteristic, response)
                is BLESyncSession.BLESyncDataAck -> manageSyncSessionDataPacketAck(device, characteristic, response)
                is BLESyncSession.BLESyncDataPacketEnd -> markSyncSessionPacketEnded(device, characteristic, response)
                is BLESyncSession.SyncPacketTransition ->
                    checkTransitionAckAndSendDataPacket(device, characteristic, response)

                else -> return Result.failure(InvalidSyncTypeException())
            }
            Result.success(Unit)
        } catch (e: SerializationException) {
            Logger.w(TAG) { "FAILED TO SERIALIZE DATA" }
            Result.failure(e)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
            Result.failure(e)
        }
    }

    private suspend fun manageSyncSessionDataPacketAck(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        data: BLESyncSession.BLESyncDataAck,
    ): Result<Boolean> {
        Logger.d(TAG) { "RECEIVED A DATA PACKET ACK TYPE:${data.type}" }
        return runCatching {
            // mark the payload as consumed
            outPayloadManager.markChunkAck(data.sequenceNumber)

            // check if wr have more bytes that can be sent
            if (!outPayloadManager.getHasMoreChunks()) {
                // send we are done with sending metadata packet
                val response = BLESyncSession.BLESyncDataPacketEnd(type = data.type)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                return@runCatching onCharacteristicsChanged?.invoke(device, characteristic, bytes) ?: false
            }

            val chunk = outPayloadManager.getNextChunk()
                .getOrElse { err ->
                    Logger.w(TAG, err) { "ISSUE WITH NEXT CHUNK" }
                    // mark this as failed
                    val response = BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.TAMPERED_DATA, true)
                    val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                    return@runCatching onCharacteristicsChanged?.invoke(device, characteristic, bytes) ?: false
                }

            // now send the response as the payload block
            val response = BLESyncSession.BLESyncDataPacket(data.type, chunk.seqNumber, chunk.payload)
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
            onCharacteristicsChanged?.invoke(device, characteristic, bytes) ?: false
        }
    }


    private suspend fun markSyncSessionPacketEnded(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        payload: BLESyncSession.BLESyncDataPacketEnd,
    ): Result<Unit> = runCatching {
        Logger.d(TAG) { "SESSION PACKET END RECEIVED TYPE:${payload.type}" }

        // send sync packet processing
        val bytes = protoBuf.encodeToByteArray<BLESyncSession>(BLESyncSession.SyncPacketProcessing)
        onCharacteristicsChanged?.invoke(device, characteristic, bytes)

        // process the payload
        val processedResult = inPayloadManager.processData()
        val data = when (val result = processedResult.getOrThrow()) {
            is SyncDataPayload.ContentIdsQuery if result.ids.isEmpty() -> {
                // DATA IS COMPLETELY SAME NO NEED TO SYNC ANYTHING
                BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.CONTENT_SAME, false)
            }

            is SyncDataPayload.ContentPayload if result.contentData.isEmpty() -> {
                // DATA IS COMPLETELY SAME NO NEED TO SYNC ANYTHING
                BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.CONTENT_SAME, false)
            }

            is SyncDataPayload.ContentIdsQuery if (payload.type == BLESyncDataType.METADATA) -> {
                // LOAD THE DATA AND TRANSITION FROM METADAT TO CONTENT_REQ
                outPayloadManager.prepareChunks(result)
                BLESyncSession.SyncPacketTransition(BLESyncDataType.METADATA, BLESyncDataType.CONTENT_REQUEST)
            }

            is SyncDataPayload.ContentPayload if payload.type == BLESyncDataType.CONTENT_REQUEST -> {
                // LOAD THE DATA AND TRANSITION FROM CONTENT REQ TO CONTENT
                outPayloadManager.prepareChunks(result)
                // now send a transition request
                BLESyncSession.SyncPacketTransition(BLESyncDataType.CONTENT_REQUEST, BLESyncDataType.CONTENT)
            }

            SyncDataPayload.SuccessAndNoAction -> {
                // Half duplex sync done
                val successBytes = protoBuf.encodeToByteArray<BLESyncSession>(BLESyncSession.SyncSessionSuccessful)
                onCharacteristicsChanged?.invoke(device, characteristic, successBytes)

                Logger.d(TAG) { "STARTING THE SECOND HALF-DUPLEX SYNC" }
                // NOW WE NEED TO SEND THE METADATA FROM THE ADVERTISER
                outPayloadManager.prepareChunks(SyncDataPayload.Metadata)
                // now send a transition request
                BLESyncSession.SyncPacketTransition(BLESyncDataType.CONTENT, BLESyncDataType.METADATA)
            }

            else -> throw Exception("Method not implemented")
        }

        val packetData = protoBuf.encodeToByteArray<BLESyncSession>(data)
        onCharacteristicsChanged?.invoke(device, characteristic, packetData)
    }

    suspend fun checkTransitionAckAndSendDataPacket(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        payload: BLESyncSession.SyncPacketTransition,
    ) = runCatching {
        // if the transition requested send an ack
        when {
            payload.isRequested -> {
                // clear the buffer and send ack
                inPayloadManager.clearBuffer()
                outPayloadManager.reset()

                val dataAck = payload.copy(isRequested = false, isAck = true)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(dataAck)
                return@runCatching onCharacteristicsChanged?.invoke(device, characteristic, bytes) ?: false
            }

            // transition need to be ack
            !payload.isAck || payload.newType == null -> {
                Logger.w(TAG) { "MISSING ACK FLAG OR NEW DATA TYPE IS NOT MENTIONED" }
                val session = BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.MISSING_FLAG, true)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(session)
                return@runCatching onCharacteristicsChanged?.invoke(device, characteristic, bytes)
                    ?: false
            }

            !outPayloadManager.getHasMoreChunks() -> {
                // send we are done with sending metadata packet
                val response = BLESyncSession.BLESyncDataPacketEnd(type = payload.newType)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                return@runCatching onCharacteristicsChanged?.invoke(device, characteristic, bytes)
                    ?: false
            }

            // now send the response
            else -> {
                val chunkResult = outPayloadManager.getNextChunk()
                // we have a block
                val chunk = chunkResult.getOrElse { err ->
                    Logger.w(TAG, err) { "A CHUNK OF DATA SHOULD BE PRESENT" }
                    val session = BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.INVALID_STATE, true)
                    val bytes = protoBuf.encodeToByteArray<BLESyncSession>(session)
                    return@runCatching onCharacteristicsChanged?.invoke(device, characteristic, bytes)
                        ?: false
                }

                // now send the response
                val response = BLESyncSession.BLESyncDataPacket(payload.newType, chunk.seqNumber, chunk.payload)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                onCharacteristicsChanged?.invoke(device, characteristic, bytes) ?: false
            }
        }
    }


    suspend fun manageSyncSessionDataPacket(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        data: BLESyncSession.BLESyncDataPacket,
    ): Result<Boolean> {
        Logger.d(TAG) { "RECEIVED A DATA PACKET TYPE:${data.type}" }

        return runCatching {
            // add the chunk to the payload
            inPayloadManager.addIncomingPayloadChunk(data.sequenceNumber, data.payload)
            val data = BLESyncSession.BLESyncDataAck(data.type, data.sequenceNumber)
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(data)
            onCharacteristicsChanged?.invoke(device, characteristic, bytes) ?: false
        }
    }


    private suspend fun sendSessionStartACK(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic)
        : Result<Boolean> {
        // clear buffers for both cases
        outPayloadManager.reset()
        inPayloadManager.clearBuffer()

        Logger.d(TAG) { "SESSION START ACK" }
        val data = BLESyncSession.SyncSessionStartAck(true)
        return runCatching {
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(data)
            onCharacteristicsChanged?.invoke(device, characteristic, bytes) ?: false
        }
    }

    fun markDeviceDisconnectedAndClearCache(address: String) {
        _localNonceMap.remove(address)
        _cccDescriptorMap.remove(address)
    }

    fun cleanUp() {
        _localNonceMap.clear()
        _cccDescriptorMap.clear()
    }

    fun handleCCCWriteRequest(
        device: BluetoothDevice,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ): Result<Unit> {

        if (descriptor.uuid.toKotlinUuid() != BLEConstants.CCC_DESCRIPTOR)
            return Result.failure(Exception("INVALID DESCRIPTOR PROVIDED ONLY CCC DESCRIPTOR ALLOWED"))

        return runCatching {
            _cccDescriptorMap[device.address] = value.btDescriptorsNotificationOrIndicationEnabled
            val bytesAsString = value.joinToString("-") { it.toHexString() }
            Logger.d(TAG) { "UPDATED DESCRIPTOR VALUE :$bytesAsString" }
        }
    }

    fun handleCCCReadRequest(
        device: BluetoothDevice,
        descriptor: BluetoothGattDescriptor,
    ): Result<ByteArray> {

        if (descriptor.uuid.toKotlinUuid() != BLEConstants.CCC_DESCRIPTOR)
            return Result.failure(Exception("INVALID DESCRIPTOR PROVIDED ONLY CCC DESCRIPTOR ALLOWED"))

        val isEnabled = _cccDescriptorMap[device.address] ?: false
        val isIndication = descriptor.characteristic.hasIndication
        val bytes = when (isEnabled) {
            true if (isIndication) -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            true -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            false -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        val bytesAsString = bytes.joinToString("-") { it.toHexString() }
        Logger.d(TAG) { "DESCRIPTOR READ VALUE : $bytesAsString" }
        return Result.success(bytes)
    }


    private class InvalidSyncTypeException : Exception("incorrect sync type")
    private class LocalDeviceInfoMissing : Exception("Local device data need to be known")
    private class MissingSavedDeviceException : Exception("Device is not saved by the user earlier")

    companion object {

        private const val NONCE_SIZE = 16
        private const val TAG = "SERVER_METHOD_HANDLER"
    }
}
