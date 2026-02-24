package com.sam.bluepad.data.ble.callbacks

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.BLEHandshakeFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.data.sync.dto.BLESyncHandshakeData
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.events.AdvertiserSyncEvent
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.toKotlinUuid

class ServerCallbackMethodHandler(
    val protoBuf: ProtoBuf,
    val randomGenerator: RandomGenerator,
    val platformInfoProvider: PlatformInfoProvider,
    val encoder: BytesEncoder,
) {

    val nonceSize = 8
    val loggingTag: String = "SERVER_METHOD_HANDLER"

    val localNonceMap = ConcurrentHashMap<String, String>()
    val cccDescriptorMap = ConcurrentHashMap<String, Boolean>()

    var notifyCharacteristicsChanged: GATTNotifyCharacteristicsChanged? = null

    val peerDeviceData = MutableStateFlow<List<BLEPeerData>>(emptyList())
    val advertiserSyncEvents = Channel<AdvertiserSyncEvent>(Channel.CONFLATED)

    inline fun handleDeviceReadRequest(
        currentDeviceInfo: LocalDeviceInfoModel? = null,
        onFailed: (String?) -> Unit = {},
    ): ByteArray? {
        if (currentDeviceInfo == null) {
            Logger.w(loggingTag) { "NO DEVICE INFO PROVIDED" }
            onFailed("No device info provided")
            return null
        }

        return try {
            val nonce = randomGenerator.generateRandomBytes(size = nonceSize)
            val peerData = BLEPeerData(
                deviceId = currentDeviceInfo.deviceId,
                deviceOs = platformInfoProvider.platformOS,
                deviceName = currentDeviceInfo.name,
                nonce = nonce.decodeToString(),
            )
            val bytes = protoBuf.encodeToByteArray<BLEPeerData>(peerData)
            Logger.d(loggingTag) { "RESPONDING WITH DATA SIZE:${bytes.size}" }
            bytes
        } catch (e: Exception) {
            Logger.w(loggingTag, e) { "UNABLE TO SERIALIZE THE DATA" }
            onFailed("SOME EXCEPTION :${e.localizedMessage ?: e.message}")
            null
        }
    }

    inline fun handleDeviceWriteRequest(
        value: ByteArray,
        onFailed: (String?) -> Unit = {},
    ) {
        try {
            val peerData = protoBuf.decodeFromByteArray<BLEPeerData>(value)
            peerDeviceData.update { devices -> (devices + peerData).distinctBy { it.deviceId } }
        } catch (e: Exception) {
            Logger.w(loggingTag, e) { "UNABLE TO SERIALIZE THE DATA" }
            onFailed("SOME EXCEPTION :${e.localizedMessage ?: e.message}")
            return
        }
    }

    inline fun handleProximityReadRequest(
        device: BluetoothDevice,
        currentDeviceInfo: LocalDeviceInfoModel? = null,
        onFailed: (String?) -> Unit = {},
    ): ByteArray? {
        if (currentDeviceInfo == null) {
            Logger.w(loggingTag) { "NO DEVICE INFO PROVIDED" }
            onFailed("No device info provided")
            return null
        }

        return try {
            val nonce = randomGenerator.generateRandomBytes(nonceSize).let { nonceBytes ->
                encoder.encodeBytes(nonceBytes).apply { localNonceMap[device.address] = this }
            }
            val data =
                BLESyncHandshakeData.AdvertiseDeviceData(currentDeviceInfo.deviceId, nonce, true)
            val bytes =
                protoBuf.encodeToByteArray<BLESyncHandshakeData.AdvertiseDeviceData>(data)
            Logger.d(loggingTag) { "RESPONDING WITH DATA SIZE:${bytes.size}" }
            bytes
        } catch (e: Exception) {
            Logger.w(loggingTag, e) { "UNABLE TO SERIALIZE THE DATA" }
            onFailed("SOME EXCEPTION :${e.localizedMessage ?: e.message}")
            null
        }
    }

    inline fun handleProximityWriteRequest(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        savedDevices: List<ExternalDeviceModel>,
        currentDeviceInfo: LocalDeviceInfoModel? = null,
        onFailed: (String?) -> Unit = {},
    ) {
        try {
            val response =
                protoBuf.decodeFromByteArray<BLESyncHandshakeData.AdvertiseResponseData>(value)

            val savedNonce = localNonceMap[device.address]
            val externalDevice = savedDevices.find { it.id == response.senderID }

            val data = when {
                savedNonce == null ->
                    BLESyncHandshakeData.HandshakeACKFailed(reason = BLEHandshakeFailedReason.INVALID_INCOMING_DATA)

                savedNonce != response.nonce ->
                    BLESyncHandshakeData.HandshakeACKFailed(BLEHandshakeFailedReason.TAMPERED_DATA)

                response.receiverID != currentDeviceInfo?.deviceId ->
                    BLESyncHandshakeData.HandshakeACKFailed(BLEHandshakeFailedReason.INVALID_RECEIVER)

                externalDevice == null ->
                    BLESyncHandshakeData.HandshakeACKFailed(BLEHandshakeFailedReason.UNKNOWN_SENDER)

                else -> BLESyncHandshakeData.HandshakeACKSuccess(nonce = response.nonce)
            }

            val bytes = protoBuf.encodeToByteArray<BLESyncHandshakeData>(data)
            notifyCharacteristicsChanged?.invoke(device, characteristic, false, bytes)

            if (externalDevice == null) {
                onFailed("RECEIVER DEVICE IS NOT KNOWN")
                return
            }
            val event = AdvertiserSyncEvent.ForeignSyncRequest(externalDevice)
            advertiserSyncEvents.trySend(event)
            when (data) {
                is BLESyncHandshakeData.HandshakeACKSuccess -> Logger.d(loggingTag) { "NOTIFICATION ON CHARACTERISTIC : ${characteristic.uuid} SEND SUCCESS" }
                is BLESyncHandshakeData.HandshakeACKFailed -> Logger.d(loggingTag) { "NOTIFICATION ON CHARACTERISTIC : ${characteristic.uuid} FAILED REASON :${data.reason}" }
                else -> {}
            }
        } catch (e: Exception) {
            Logger.w(loggingTag, e) { "UNABLE TO SERIALIZE THE DATA" }
            onFailed("SOME EXCEPTION :${e.localizedMessage ?: e.message}")
            return
        }
    }

    inline fun handleDataWriteRequest(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        onSessionStart: () -> Unit = {},
        onReadPayload: (BLESyncDataType, Int, String) -> Unit,
        onPayloadTypeChange: (BLESyncDataType?) -> Unit = {},
        onFailed: (String?) -> Unit = {},
    ) {
        val isIndication = characteristic.properties and
                BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        try {
            // read the response
            when (val response = protoBuf.decodeFromByteArray<BLESyncSession>(value)) {
                BLESyncSession.SyncSessionStart -> {
                    onSessionStart()
                    // TODO: HANDLE IF NOT HANDLED CORRECTLY
                    val bytes = protoBuf.encodeToByteArray<BLESyncSession>(
                        BLESyncSession.SyncSessionStartAck(true)
                    )
                    notifyCharacteristicsChanged?.invoke(
                        device,
                        characteristic,
                        isIndication,
                        bytes
                    )
                }

                is BLESyncSession.BLESyncDataPacket -> {
                    onReadPayload(response.type, response.seqNumber, response.payload)

                    val bytes = protoBuf.encodeToByteArray<BLESyncSession.BLESyncDataAck>(
                        BLESyncSession.BLESyncDataAck(response.type, response.seqNumber)
                    )
                    // notify with acknowledgement some data is received
                    notifyCharacteristicsChanged?.invoke(
                        device,
                        characteristic,
                        isIndication,
                        bytes
                    )
                }

                is BLESyncSession.SyncPacketSwitch -> onPayloadTypeChange(response.newType)
                else -> onFailed("Invalid sync session type")
            }
            advertiserSyncEvents.trySend(AdvertiserSyncEvent.ForeignDeviceExchangingData)
        } catch (e: Exception) {
            Logger.w(loggingTag, e) { "UNABLE TO SERIALIZE THE DATA" }
            onFailed("SOME EXCEPTION :${e.localizedMessage ?: e.message}")
            return
        }
    }

    inline fun handleCCCWriteRequest(
        device: BluetoothDevice,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
        onFailed: (String?) -> Unit = {},
    ) {
        if (descriptor.uuid.toKotlinUuid() != BLEConstants.CCC_DESCRIPTOR) {
            onFailed("INVALID DESCRIPTOR PROVIDED ONLY CCC DESCRIPTOR ALLOWED")
            return
        }

        val isNotifyEnabled = when {
            value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) -> true
            value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) -> true
            value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) -> false
            else -> {
                onFailed("Invalid CCC Value")
                return
            }
        }

        cccDescriptorMap[device.address] = isNotifyEnabled
        val bytesAsString = value.joinToString("-") { it.toHexString() }
        Logger.d(loggingTag) { "UPDATED DESCRIPTOR VALUE :$bytesAsString" }
    }

    inline fun handleCCCReadRequest(
        device: BluetoothDevice,
        descriptor: BluetoothGattDescriptor,
        onFailed: (String?) -> Unit = {},
    ): ByteArray? {
        if (descriptor.uuid.toKotlinUuid() != BLEConstants.CCC_DESCRIPTOR) {
            onFailed("INVALID DESCRIPTOR PROVIDED ONLY CCC DESCRIPTOR ALLOWED")
            return null
        }

        val isEnabled = cccDescriptorMap[device.address] ?: false
        val isIndication =
            descriptor.characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        val bytes = when (isEnabled) {
            true if (isIndication) -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            true -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            false -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        val bytesAsString = bytes.joinToString("-") { it.toHexString() }
        Logger.d(loggingTag) { "DESCRIPTOR READ VALUE : $bytesAsString" }
        return bytes
    }

    fun clearDeviceInfo(address: String) {
        localNonceMap.remove(address)
        cccDescriptorMap.remove(address)
    }

    fun cleanUp() {
        localNonceMap.clear()
        cccDescriptorMap.clear()
    }
}