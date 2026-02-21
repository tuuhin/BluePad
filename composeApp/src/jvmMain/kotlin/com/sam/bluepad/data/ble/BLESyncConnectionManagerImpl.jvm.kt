package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.juul.kable.Advertisement
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.logs.Logging
import com.sam.ble_common.BluetoothInfoProvider
import com.sam.bluepad.data.sync.dto.BLESyncACKFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncData
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLESyncConnectionManager
import com.sam.bluepad.domain.ble.ResourcesSyncDataEvents
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.exceptions.BLENotSupportedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val TAG = "BLE_SYNC_MANAGER"

actual class BLESyncConnectionManagerImpl(
    private val protoBuf: ProtoBuf,
    private val localDeviceProvider: LocalDeviceInfoProvider,
    private val externalDevicesRepository: ExternalDevicesRepository,
) : BLESyncConnectionManager {

    private val _scanner = Scanner {
        logging {
            format = Logging.Format.Multiline
            level = Logging.Level.Warnings
        }

        filters {
            match {
                services += listOf(BLEConstants.SYNC_SERVICE_ID)
            }
        }
    }

    override fun discoverAndConnect(timeout: Duration): Flow<ResourcesSyncDataEvents> =
        flow<ResourcesSyncDataEvents> {

            if (!BluetoothInfoProvider.isBluetoothActive()) {
                emit(Resource.Error(BluetoothNotEnabledException()))
                return@flow
            }

            if (!BluetoothInfoProvider.isLEConnectionAllowed()) {
                emit(Resource.Error(BLENotSupportedException()))
                return@flow
            }

            try {
                emit(Resource.Success(ConnectorSyncEvent.DiscoveryStarted))
                Logger.i(TAG) { "SCANNING FOR DEVICES STATED" }
                val firstAdvertisement = withTimeout(timeout) {
                    _scanner.advertisements
                        // an extra delay for the hardware to cool down
                        .onEach { delay(100) }
                        .first()
                }
                Logger.i(TAG) { "SCAN RESULT FOUND" }
                val identifier = firstAdvertisement.identifier.toString()
                emit(Resource.Success(ConnectorSyncEvent.DeviceFound(identifier)))
                // deals with the connection
                val connectionFlow = handleConnection(firstAdvertisement)
                emitAll(connectionFlow.map { Resource.Success(it) })
            } catch (_: TimeoutCancellationException) {
                Logger.w(TAG) { "SCAN TIMEOUT ADVERTISEMENT NOT FOUND FOR TIMEOUT: $timeout" }
                emit(Resource.Success(ConnectorSyncEvent.DeviceScanTimeout))
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Logger.d(TAG) { "CONNECTION JOB CANCELLED" }
                    throw e
                }
                emit(Resource.Error(e))
                Logger.e(TAG, e) { "Some exception" }
            }
        }.flowOn(Dispatchers.IO)


    private fun handleConnection(
        advertisement: Advertisement,
        connectionTimeout: Duration = 20.seconds
    ): Flow<ConnectorSyncEvent> {

        val peripheral = Peripheral(advertisement) {
            this.disconnectTimeout = 10.seconds
            this.forceCharacteristicEqualityByUuid = true
            logging {
                identifier = "IDENTIFIER: ${advertisement.identifier}"
                format = Logging.Format.Compact
                level = Logging.Level.Events
            }
            // exception handler
            observationExceptionHandler { exp ->
                Logger.e(TAG, exp) { "EXCEPTION HAPPENED" }
            }
            onServicesDiscovered {
                Logger.d(TAG) { "SERVICE DISCOVERED SERVICE" }
            }
        }

        return channelFlow {
            try {
                withTimeout(connectionTimeout) {
                    peripheral.connect()
                    send(ConnectorSyncEvent.ConnectionSuccess)
                    Logger.i(TAG) { "PERIPHERAL CONNECTION ESTABLISHED" }
                }
                // read the required characteristic
                val services = peripheral.services.value ?: emptyList()
                val syncCharacteristics = services
                    .find { it.serviceUuid == BLEConstants.SYNC_SERVICE_ID }
                    ?.characteristics
                    ?.find { it.characteristicUuid == BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID }

                if (syncCharacteristics == null) {
                    Logger.w(TAG) { "REQUIRED CHARACTERISTIC MISSING" }
                    close(MissingSyncCharacteristicsException())
                    return@channelFlow
                }

                // handle the notifications
                peripheral.observe(syncCharacteristics).onEach { bytes ->
                    val result = try {
                        protoBuf.decodeFromByteArray<BLESyncData>(bytes)
                    } catch (e: SerializationException) {
                        Logger.e(TAG, e) { "CANNOT SERIALIZE THE DATA" }
                        null
                    } catch (e: IllegalArgumentException) {
                        Logger.e(TAG, e) { "INVALID INPUT" }
                        null
                    }
                    when (result) {
                        is BLESyncData.BLESyncACKFailed -> {
                            Logger.d(TAG) { "FAILED ACKNOWLEDGEMENT FOUND REASON:${result.reason}" }
                            close(InvalidAcknowledgementException(result.reason))
                        }

                        is BLESyncData.BLESyncACKSuccess -> {
                            Logger.i(TAG) { "ACK FOUND DEVICE ADDRESS: ${result.deviceAddress}" }
                            send(ConnectorSyncEvent.AdvertisingAcknowledgmentReceived)
                        }

                        else -> {}
                    }
                }.launchIn(peripheral.scope)

                val bytes = peripheral.read(syncCharacteristics)
                val syncData = protoBuf.decodeFromByteArray<BLESyncData.BLEAdvertiseData>(bytes)

                if (!syncData.allowSync) {
                    close(SyncFlagMissingException())
                    return@channelFlow
                }

                Logger.i(TAG) { "CHARACTERISTICS :${syncCharacteristics.characteristicUuid} READ SUCCESS" }
                val deviceResult = externalDevicesRepository.getDeviceByUuid(syncData.deviceId)

                if (deviceResult.isFailure) {
                    Logger.w(TAG) { "CANNOT FIND THE GIVEN DEVICE " }
                    close(InvalidReceiverIdException())
                    return@channelFlow
                }

                val event = ConnectorSyncEvent.AdvertisingDeviceRead(device = deviceResult.getOrThrow())
                send(event)

                val currentDeviceInfo = localDeviceProvider.readDeviceInfo.first()
                val outgoingData = BLESyncData.BLEAdvertiseResponse(
                    nonce = syncData.nonce,
                    receiverID = syncData.deviceId,
                    senderID = currentDeviceInfo.deviceId
                )
                val syncWrite = protoBuf.encodeToByteArray(outgoingData)

                peripheral.write(syncCharacteristics, syncWrite)
                send(ConnectorSyncEvent.ConnectorDeviceDataResponseSend)
                Logger.i(TAG) { "DEVICE INFO WRITE" }

            } catch (_: TimeoutCancellationException) {
                send(ConnectorSyncEvent.DeviceScanTimeout)
                Logger.i(TAG) { "FAILED TO CONNECT TO THE DEVICE TIMEOUT OCCURRED" }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(TAG, e) { "UNKNOWN EXCEPTION" }
            } finally {
                try {
                    Logger.i(TAG) { "DISCONNECTING THE PERIPHERAL" }
                    peripheral.disconnect()
                } catch (_: CancellationException) {
                    Logger.w(TAG) { "FAILED TO DISCONNECT THE PERIPHERAL" }
                }
            }
            awaitClose {
                Logger.i(TAG) { "PERIPHERAL CONNECTION CLOSED" }
                peripheral.close()
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun close() = Unit

    private class MissingSyncCharacteristicsException : Exception("Missing advertisement data")
    private class InvalidReceiverIdException : Exception("Invalid receiver id provided")
    private class InvalidAcknowledgementException(reason: BLESyncACKFailedReason) :
        Exception("Invalid Acknowledgement :${reason.name}")

    private class SyncFlagMissingException : Exception("No sync flag found in the read response")
}