package com.sam.bluepad.data.ble.delegate

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.exceptions.BLEConnectorException
import com.sam.bluepad.data.sync.dto.BLESyncHandshakeData
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.uuid.Uuid

class PeerProximityConnectorDelegate(
    private val protoBuf: ProtoBuf,
    private val dispatchers: PlatformDispatcherProvider
) {

    private val _lock = Mutex()
    private val _handshakeMap = HashMap<String, BLESyncHandshakeData.AdvertiseResponseData>()
    private val _handshakeNotificationMap = HashMap<String, Boolean>()

    suspend fun handleHandshakeRead(
        deviceAddress: String,
        value: ByteArray,
        deviceInfo: LocalDeviceInfoModel?,
        savedDevices: suspend (Uuid) -> Result<ExternalDeviceModel>,
        onReadSuccess: () -> Boolean,
    ): Result<ExternalDeviceModel> = runCatching {

        val currentDeviceInfo = deviceInfo ?: throw BLEConnectorException.LocalDeviceInfoMissing()
        val syncData = withContext(dispatchers.io) {
            protoBuf.decodeFromByteArray<BLESyncHandshakeData.AdvertiseDeviceData>(value)
        }

        if (!syncData.allowSync) {
            Logger.e(tag = TAG) { "SYNC FLAG MISSING" }
            throw BLEConnectorException.SyncFlagMissingException()
        }

        val externalDevice = savedDevices(syncData.deviceId).getOrElse { err ->
            Logger.w(tag = TAG) { "CANNOT FIND THE GIVEN DEVICE " }
            return Result.failure(err)
        }

        Logger.d(tag = TAG) { "ADVERTISE DATA RECEIVED DEVICE_ID:${syncData.deviceId} VERIFIED" }
        // on write notification fully active we will send the outgoing data
        onReadSuccess()

        val outgoingData = BLESyncHandshakeData.AdvertiseResponseData(
            nonce = syncData.nonce,
            receiverID = syncData.deviceId,
            senderID = currentDeviceInfo.deviceId,
        )

        _lock.withLock {
            // saving the content data on the cache map
            _handshakeMap[deviceAddress] = outgoingData
            _handshakeNotificationMap[deviceAddress] = true
        }
        externalDevice

    }

    suspend fun handleHandshakeNotification(
        value: ByteArray,
        onHandshakeSuccess: suspend () -> Boolean
    ): Result<Unit> = runCatching {

        val result = withContext(dispatchers.io) {
            protoBuf.decodeFromByteArray<BLESyncHandshakeData>(value)
        }
        Logger.i(tag = TAG) { "HANDSHAKE ACK DATA FOUND" }
        // handle the result
        when (result) {
            is BLESyncHandshakeData.HandshakeACKFailed -> {
                val error = BLEConnectorException.InvalidAcknowledgementException(result.reason)
                Logger.w(tag = TAG, throwable = error) { "FAILED ACKNOWLEDGEMENT FOUND REASON:${result.reason}" }
                throw error
            }

            is BLESyncHandshakeData.HandshakeACKSuccess -> {
                Logger.i(tag = TAG) { "HANDSHAKE SUCCESSFUL TURING OFF ADVERTISEMENTS" }
                // send this after some time to clear the bluetooth stack
                onHandshakeSuccess()
            }

            else -> throw BLEConnectorException.InvalidHandshakeValueException()
        }

    }

    suspend fun onEnabledDisabledCCCDescriptor(
        address: String,
        characteristicId: Uuid,
        bytes: ByteArray,
        onWriteBytes: suspend (ByteArray) -> Boolean,
        onToggleNotification: suspend (characteristics: Uuid, enable: Boolean) -> Boolean,
    ) {
        val isEnabled = bytes.btDescriptorsNotificationOrIndicationEnabled

        when (characteristicId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if isEnabled -> {
                // thus notification is turned on successfully
                val outgoingData = _lock.withLock { _handshakeMap[address] } ?: return
                val syncWrite = protoBuf.encodeToByteArray<BLESyncHandshakeData.AdvertiseResponseData>(outgoingData)
                val response = onWriteBytes(syncWrite)
                Logger.d(tag = TAG) { "WRITING ADVERTISING RESPONSE CHARACTERISTICS IS_SUCCESS:$response" }
            }

            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID -> {
                _lock.withLock {
                    val isNotificationOn = _handshakeNotificationMap[address] ?: false
                    if (!isNotificationOn) return
                    _handshakeNotificationMap.remove(address)
                }
                Logger.d(tag = TAG) { "TURNING OFF HANDSHAKE NOTIFICATION AND TURNING ON DATA NOTIFICATION" }

                onToggleNotification(BLEConstants.SYNC_DATA_CHARACTERISTICS_ID, true)
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if isEnabled -> {
                Logger.d(tag = TAG) { "STARTING CHARACTERISTICS NOTIFICATION TURNED ON" }
                _lock.withLock {
                    val sessionId = Uuid.random()
                    val sendData = BLESyncSession.SyncSessionStart(sessionId = sessionId)
                    val bytesToSend = protoBuf.encodeToByteArray<BLESyncSession>(sendData)
                    onWriteBytes(bytesToSend)
                }
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> Logger.d(tag = TAG) { "SYNCING IS DONE NOW SYNC NOTIFICATION ARE DISMISSED" }
            else -> {
                val text = if (isEnabled) "ENABLED" else "DISABLED"
                Logger.d(tag = TAG) { "GATT NOTIFICATION $text  FOR CHARACTERISTICS :${characteristicId}" }
            }
        }
    }

    companion object {
        private const val TAG = "PEER_PROXIMITY_CONNECTOR_DELEGATE"
    }
}
