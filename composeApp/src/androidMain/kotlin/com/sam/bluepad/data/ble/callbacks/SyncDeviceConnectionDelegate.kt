package com.sam.bluepad.data.ble.callbacks

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Handler
import android.os.Looper
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.utils.toggleNotification
import com.sam.bluepad.data.ble.utils.writeToCharacteristics
import com.sam.bluepad.data.sync.dto.BLEHandshakeFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.data.sync.dto.BLESyncFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncHandshakeData
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.exceptions.EmptyPayloadException
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@SuppressLint("MissingPermission")
class SyncDeviceConnectionDelegate(
    val protoBuf: ProtoBuf,
    val outPayloadManager: OutPayloadManager,
    val inPayloadManager: InPayloadManager,
) {

    val handler = Handler(Looper.getMainLooper())

    val handShakeDataMap = ConcurrentHashMap<String, BLESyncHandshakeData.AdvertiseResponseData>()
    val hadShakeNotificationMap = ConcurrentHashMap<String, Boolean>()

    fun requestHandshakeCharacteristics(gatt: BluetoothGatt): Result<Unit> {

        val syncService = gatt.getService(BLEConstants.SYNC_SERVICE_ID.toJavaUuid())
            ?: run {
                val ex = MissingServiceOrCharacteristics(true, BLEConstants.SYNC_SERVICE_ID)
                Logger.w(TAG, ex) { "SYNC SERVICE NOT FOUND" }
                return Result.failure(ex)
            }

        val characteristic = syncService
            .getCharacteristic(BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID.toJavaUuid())
            ?: run {
                val ex = MissingServiceOrCharacteristics(true, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID)
                Logger.w(TAG, ex) { "MISSING CHARACTERISTICS" }
                return Result.failure(ex)
            }

        return runCatching {
            val isSuccess = gatt.readCharacteristic(characteristic)
            Logger.d(TAG) { "READ CHARACTERISTICS OP SUCCESS: $isSuccess" }
        }
    }

    suspend inline fun handleHandshakeRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        deviceInfo: LocalDeviceInfoModel?,
        savedDevices: suspend (Uuid) -> Result<ExternalDeviceModel>,
    ): Result<ExternalDeviceModel> {
        return runCatching {

            val currentDeviceInfo = deviceInfo ?: throw LocalDeviceInfoMissing()
            val syncData = protoBuf.decodeFromByteArray<BLESyncHandshakeData.AdvertiseDeviceData>(value)

            if (!syncData.allowSync) {
                Logger.e(TAG) { "SYNC FLAG MISSING" }
                throw SyncFlagMissingException()
            }

            val externalDevice = savedDevices(syncData.deviceId).getOrElse { err ->
                Logger.w(TAG) { "CANNOT FIND THE GIVEN DEVICE " }
                return Result.failure(err)
            }

            Logger.d(TAG) { "ADVERTISE DATA RECEIVED DEVICE_ID:${syncData.deviceId} VERIFIED" }
            // on write notification fully active we will send the outgoing data
            handler.postDelayed(
                { gatt.toggleNotification(characteristic, true) },
                100,
            )

            val outgoingData = BLESyncHandshakeData.AdvertiseResponseData(
                nonce = syncData.nonce,
                receiverID = syncData.deviceId,
                senderID = currentDeviceInfo.deviceId,
            )

            // saving the content data on the cache map
            val address = gatt.device.address
            handShakeDataMap[address] = outgoingData
            hadShakeNotificationMap[address] = true

            externalDevice
        }
    }


    fun handleHandshakeNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Result<ConnectorSyncEvent> {
        try {
            val result = protoBuf.decodeFromByteArray<BLESyncHandshakeData>(value)
            Logger.i(TAG) { "HANDSHAKE ACK DATA FOUND" }
            // handle the result
            when (result) {
                is BLESyncHandshakeData.HandshakeACKFailed -> {
                    val error = InvalidAcknowledgementException(result.reason)
                    Logger.d(TAG, error) { "FAILED ACKNOWLEDGEMENT FOUND REASON:${result.reason}" }
                    return Result.failure(error)
                }

                is BLESyncHandshakeData.HandshakeACKSuccess -> {
                    Logger.i(TAG) { "HANDSHAKE SUCCESSFUL TURING OFF ADVERTISEMENTS" }
                    // send this after some time to clear the bluetooth stack
                    handler.postDelayed({ gatt.toggleNotification(characteristic, false) }, 200)
                    return Result.success(ConnectorSyncEvent.AdvertisingAcknowledgmentReceived)
                }

                else -> {}
            }

        } catch (e: SerializationException) {
            Logger.e(TAG, e) { "CANNOT SERIALIZE THE DATA" }
        } catch (e: Exception) {
            Logger.e(TAG, e) { "UNKNOWN EXCEPTION" }
        }
        return Result.failure(InvalidHandshakeValueException())
    }

    suspend inline fun handleSyncDataNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        onEvent: (ConnectorSyncEvent) -> Unit,
        onError: (Throwable?) -> Unit,
    ) {
        try {
            val decodedData = protoBuf.decodeFromByteArray<BLESyncSession>(value)
            Logger.d(TAG) { "SYNC NOTIFICATION RECEIVED DATA | SIZE:${value.size}" }

            val result = when (decodedData) {
                is BLESyncSession.SyncSessionStartAck -> onSessionStartACK(gatt, characteristic, decodedData)
                is BLESyncSession.BLESyncDataPacket -> onDataPacketReceived(gatt, characteristic, decodedData)
                is BLESyncSession.BLESyncDataAck -> onDataPacketACKReceived(gatt, characteristic, decodedData)
                is BLESyncSession.BLESyncDataPacketEnd -> onDataPacketEnd(gatt, characteristic, decodedData)
                is BLESyncSession.SyncPacketTransition -> onPacketTransition(gatt, characteristic, decodedData)
                BLESyncSession.SyncSessionSuccessful -> runCatching {
                    Logger.d(TAG) { "SYNC SESSION COMPLETED" }
                }

                is BLESyncSession.SyncSessionFailed -> runCatching {
                    Logger.d(TAG) { "SYNC SESSION FAILED" }
                    gatt.toggleNotification(characteristic, false)
                }

                BLESyncSession.SyncPacketProcessing -> {
                    Logger.d(TAG) { "REMOTE PROCESSING DATA RUNNING...." }
                    onEvent(ConnectorSyncEvent.RemoteProcessing)
                    Result.success(true)
                }

                else -> Result.failure(InvalidSessionTypeException())
            }

            if (result.isFailure) {
                onError(result.exceptionOrNull())
                return
            }
        } catch (_: SerializationException) {
            Logger.e(TAG) { "INVALID DATA RECEIVED CANNOT DECODE IT" }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(TAG, e) { "UNKNOWN EXCEPTION" }
        }
    }


    suspend fun onDataPacketEnd(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: BLESyncSession.BLESyncDataPacketEnd,
    ) = runCatching {

        val bytes = protoBuf.encodeToByteArray<BLESyncSession>(BLESyncSession.SyncPacketProcessing)
        gatt.writeToCharacteristics(characteristic, bytes)

        Logger.d(TAG) { "PACKET END MARKER RECEIVED TYPE:${data.type}" }
        val result = inPayloadManager.processData().getOrThrow()

        Logger.d(TAG) { "PROCESSED RESULT:$result" }
        if (result is SyncDataPayload.Outgoing) outPayloadManager.prepareChunks(result)

        // handle the result
        val transitionPacket = when (result) {
            is SyncDataPayload.ContentPayload if (data.type == BLESyncDataType.CONTENT_REQUEST) ->
                BLESyncSession.SyncPacketTransition(BLESyncDataType.CONTENT_REQUEST, BLESyncDataType.CONTENT)

            is SyncDataPayload.ContentIdsQuery if (data.type == BLESyncDataType.METADATA) ->
                BLESyncSession.SyncPacketTransition(BLESyncDataType.METADATA, BLESyncDataType.CONTENT_REQUEST)

            is SyncDataPayload.SuccessAndNoAction -> {
                val packetBytes = protoBuf.encodeToByteArray<BLESyncSession>(BLESyncSession.SyncSessionSuccessful)
                return@runCatching gatt.writeToCharacteristics(characteristic, packetBytes)
            }

            else -> throw InvalidPayloadDataException()
        }

        val packetBytes = protoBuf.encodeToByteArray<BLESyncSession>(transitionPacket)
        gatt.writeToCharacteristics(characteristic, packetBytes)

    }


    suspend fun onPacketTransition(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: BLESyncSession.SyncPacketTransition,
    ): Result<Boolean> {
        Logger.d(TAG) { "PACKET TYPE TRANSITION TO ${data.newType} FROM :${data.prevType}" }

        return runCatching {
            when {
                // if the is request send ack
                data.isRequested -> {
                    // clear the buffer and send ack
                    inPayloadManager.clearBuffer()
                    outPayloadManager.reset()

                    val dataAck = data.copy(isRequested = false, isAck = true)
                    val bytes = protoBuf.encodeToByteArray<BLESyncSession>(dataAck)
                    gatt.writeToCharacteristics(characteristic, bytes)
                }

                // if no ack flag found
                !data.isAck -> {
                    Logger.w(TAG) { "MISSING ACK FLAG STOPPING SYNC SESSION" }
                    val session = BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.MISSING_FLAG, true)
                    val bytes = protoBuf.encodeToByteArray<BLESyncSession>(session)
                    gatt.writeToCharacteristics(characteristic, bytes)
                }

                // if no chunk data present
                !outPayloadManager.getHasMoreChunks() -> {
                    // send we are done with sending metadata packet
                    val response = BLESyncSession.BLESyncDataPacketEnd(type = data.newType!!)
                    val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                    gatt.writeToCharacteristics(characteristic, bytes)
                }

                else -> {
                    // now send the response
                    val chunkResult = outPayloadManager.getNextChunk()
                    val chunk = chunkResult.getOrElse { err ->
                        Logger.w(TAG, err) { "A CHUNK OF DATA SHOULD BE PRESENT" }
                        val session = BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.INVALID_STATE, true)
                        val bytes = protoBuf.encodeToByteArray<BLESyncSession>(session)
                        return@runCatching gatt.writeToCharacteristics(characteristic, bytes)
                    }

                    // now send the response
                    val response = BLESyncSession.BLESyncDataPacket(
                        BLESyncDataType.CONTENT_REQUEST,
                        chunk.seqNumber,
                        chunk.payload,
                    )
                    val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                    gatt.writeToCharacteristics(characteristic, bytes)
                }
            }
        }
    }

    suspend fun onDataPacketReceived(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: BLESyncSession.BLESyncDataPacket,
    ): Result<Boolean> {
        Logger.d(TAG) { "RECEIVED PACKET DATA FROM OTHER DEVICE TYPE:${data.type}" }

        return runCatching {
            inPayloadManager.addIncomingPayloadChunk(data.sequenceNumber, data.payload)
            val data = BLESyncSession.BLESyncDataAck(data.type, data.sequenceNumber)
            val sessionData = protoBuf.encodeToByteArray<BLESyncSession>(data)
            gatt.writeToCharacteristics(characteristic, sessionData)
        }
    }

    suspend fun onDataPacketACKReceived(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: BLESyncSession.BLESyncDataAck,
    ): Result<Boolean> {
        Logger.d(TAG) { "RECEIVED PACKET ACK DATA FROM OTHER DEVICE" }
        // mark the payload as consumed
        outPayloadManager.markChunkAck(data.sequenceNumber)

        if (!outPayloadManager.getHasMoreChunks()) {
            // send we are done with sending metadata packet
            return runCatching {
                val response = BLESyncSession.BLESyncDataPacketEnd(type = data.type)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                gatt.writeToCharacteristics(characteristic, bytes)
            }
        }
        val chunkResult = outPayloadManager.getNextChunk()
        // we have a block
        val chunk = chunkResult.getOrElse { err ->
            Logger.w(TAG, err) { "ISSUE WITH NEXT CHUNK" }
            return Result.failure(err)
        }
        val response = BLESyncSession.BLESyncDataPacket(
            type = data.type,
            sequenceNumber = chunk.seqNumber,
            payload = chunk.payload,
        )
        // now send the response
        return runCatching {
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
            gatt.writeToCharacteristics(characteristic, bytes)
        }
    }

    suspend fun onSessionStartACK(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        response: BLESyncSession.SyncSessionStartAck,
    ): Result<Boolean> {
        // response ack flag should be true
        if (!response.isAck) return Result.failure(SyncStarkNotAckException())

        outPayloadManager.prepareChunks(SyncDataPayload.Metadata).getOrElse { err ->
            Logger.w(TAG) { "CANNOT PREPARE THE BLOCKS" }
            return Result.failure(err)
        }

        // chunks should be probably ready by now
        if (!outPayloadManager.getHasMoreChunks()) return Result.failure(EmptyPayloadException())
        val chunk = outPayloadManager.getNextChunk()
            .getOrElse { err ->
                Logger.w(TAG, err) { "ISSUE WITH NEXT CHUNK" }
                return Result.failure(err)
            }
        // we have a block
        val response = BLESyncSession.BLESyncDataPacket(BLESyncDataType.METADATA, chunk.seqNumber, chunk.payload)
        Logger.d(TAG) { "SENDING FIRST BLOCK OF METADATA CHUNKS" }
        // now send the response
        return runCatching {
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
            gatt.writeToCharacteristics(characteristic, bytes)
        }
    }

    fun onEnabledDisabledCCCDescriptor(
        gatt: BluetoothGatt,
        characteristics: BluetoothGattCharacteristic,
        bytes: ByteArray,
    ) {
        val isEnabled = bytes.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) ||
            bytes.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)

        val address = gatt.device.address ?: return

        val characteristicId = characteristics.uuid.toKotlinUuid()
        when (characteristicId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if isEnabled -> {
                // thus notification is turned on successfully
                val outgoingData = handShakeDataMap[address] ?: return
                val syncWrite = protoBuf.encodeToByteArray<BLESyncHandshakeData.AdvertiseResponseData>(outgoingData)
                val response = gatt.writeToCharacteristics(characteristics, syncWrite)
                Logger.d(TAG) { "WRITING ADVERTISING RESPONSE CHARACTERISTICS IS_SUCCESS:$response" }
            }

            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID -> {
                val isNotificationOn = hadShakeNotificationMap[address] ?: false
                if (!isNotificationOn) return
                hadShakeNotificationMap.remove(address)
                Logger.d(TAG) { "TURNING OF HANDSHAKE NOTIFICATION AND TURNING ON DATA NOTIFICATION" }

                val syncCharacteristic = characteristics.service
                    .getCharacteristic(BLEConstants.SYNC_DATA_CHARACTERISTICS_ID.toJavaUuid())
                    ?: return

                gatt.toggleNotification(syncCharacteristic, true)
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if isEnabled -> {
                Logger.d(TAG) { "STARTING CHARACTERISTICS NOTIFICATION TURNED ON" }
                val sendData = BLESyncSession.SyncSessionStart
                val bytesToSend = protoBuf.encodeToByteArray<BLESyncSession>(sendData)
                gatt.writeToCharacteristics(characteristics, bytesToSend)
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> Logger.d(TAG) { "STOPPING SYNC SESSION" }
            else -> {
                val text = if (isEnabled) "ENABLED" else "DISABLED"
                Logger.d(TAG) { "GATT NOTIFICATION $text  FOR CHARACTERISTICS :${characteristics.uuid}" }
            }
        }

    }

    // exceptions internal
    class InvalidSessionTypeException : Exception("Provided session type is invalid or any handler is not present")
    private class MissingServiceOrCharacteristics(isService: Boolean, uuid: Uuid) :
        Exception("Missing required ${if (isService) "SERVICE" else "CHARACTERISTICS"} UUID : ${uuid.toHexString()}")

    private class SyncStarkNotAckException : Exception("Start is not ack properly missing ack flag")
    private class InvalidHandshakeValueException : Exception("Invalid Handshake value")
    private class InvalidAcknowledgementException(reason: BLEHandshakeFailedReason) :
        Exception("Invalid Acknowledgement :${reason.name}")

    private class InvalidPayloadDataException : Exception("Invalid payload type its not supported")

    class SyncFlagMissingException : Exception("No sync flag found in the read response")
    class LocalDeviceInfoMissing : Exception("Local device data need to be known")

    companion object {
        const val TAG = "SYNC_DEVICE_CONNECTION_DELEGATE"
    }
}
