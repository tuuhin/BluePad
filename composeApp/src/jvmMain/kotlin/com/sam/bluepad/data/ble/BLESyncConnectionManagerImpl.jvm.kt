package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.juul.kable.Advertisement
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.logs.Logging
import com.sam.ble_common.BluetoothInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLESyncConnectionManager
import com.sam.bluepad.domain.ble.ResourcesSyncDataEvents
import com.sam.bluepad.domain.ble.models.BLEDeviceSyncEvent
import com.sam.bluepad.domain.ble.models.BLESyncACKFailedReason
import com.sam.bluepad.domain.ble.models.BLESyncData
import com.sam.bluepad.domain.exceptions.BLENotSupportedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val TAG = "BLE_SYNC_MANAGER"

actual class BLESyncConnectionManagerImpl(
    private val protoBuf: ProtoBuf,
    private val localDeviceProvider: LocalDeviceInfoProvider,
    private val externalDevicesRepository: ExternalDevicesRepository,
) : BLESyncConnectionManager {

    private val _scanner = Scanner {
        logging {
            identifier = "SCANNER"
            format = Logging.Format.Compact
            level = Logging.Level.Events
        }

        filters {
            match {
                services += listOf(BLEConstants.SYNC_SERVICE_ID)
            }
        }
    }

    override fun discoverAndConnect(timeout: Duration): Flow<ResourcesSyncDataEvents> = flow {

        if (!BluetoothInfoProvider.isBluetoothActive()) {
            emit(Resource.Error(BluetoothNotEnabledException()))
            return@flow
        }

        if (!BluetoothInfoProvider.isLEConnectionAllowed()) {
            emit(Resource.Error(BLENotSupportedException()))
            return@flow
        }

        try {
            emit(Resource.Success(BLEDeviceSyncEvent.DiscoveryStarted))
            Logger.i(TAG) { "SCANNING FOR DEVICES STATED" }
            val firstAdvertisement = withTimeout(timeout) {
                _scanner.advertisements.first()
            }
            Logger.i(TAG) { "SCAN RESULT FOUND" }
            val identifier = firstAdvertisement.identifier.toString()
            emit(Resource.Success(BLEDeviceSyncEvent.DeviceFound(identifier)))
            // deals with the connection
            val connectionFlow = handleConnection(firstAdvertisement)
            emitAll(connectionFlow)
        } catch (_: TimeoutCancellationException) {
            Logger.w(TAG) { "SCAN TIMEOUT ADVERTISEMENT NOT FOUND FOR TIMEOUT: $timeout" }
            emit(Resource.Success(BLEDeviceSyncEvent.DeviceScanTimeout))
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
        connectionTimeout: Duration = 1.minutes
    ): Flow<ResourcesSyncDataEvents> {

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

        return channelFlow<ResourcesSyncDataEvents> {
            try {
                withTimeout(connectionTimeout) {
                    peripheral.connect()
                    send(Resource.Success(BLEDeviceSyncEvent.ConnectionSuccess))
                    Logger.i(TAG) { "PERIPHERAL CONNECTION ESTABLISHED" }
                }
                // read the required characteristic
                val services = peripheral.services.value ?: emptyList()
                val syncCharacteristics = services
                    .find { it.serviceUuid == BLEConstants.SYNC_SERVICE_ID }
                    ?.characteristics
                    ?.find { it.characteristicUuid == BLEConstants.SYNC_CHARACTERISTICS_ID }

                if (syncCharacteristics == null) {
                    Logger.w(TAG) { "REQUIRED CHARACTERISTIC MISSING" }
                    trySend(Resource.Error(MissingSyncCharacteristicsException()))
                    peripheral.close()
                    close()
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
                            Logger.i(TAG) { "ACK FOUND" }
                            val ack = BLEDeviceSyncEvent.AdvertisingAcknowledgmentReceived(result)
                            send(Resource.Success(ack))
                        }

                        else -> {}
                    }
                }.launchIn(peripheral.scope)

                val bytes = peripheral.read(syncCharacteristics)
                val syncData = protoBuf.decodeFromByteArray<BLESyncData.BLEAdvertiseData>(bytes)
                val event = BLEDeviceSyncEvent.AdvertisingDataRead(
                    characteristicsId = syncCharacteristics.characteristicUuid,
                    data = syncData
                )
                send(Resource.Success(event))

                if (!syncData.allowSync) throw SyncFlagMissingException()

                Logger.i(TAG) { "CHARACTERISTICS :${syncCharacteristics.characteristicUuid} READ SUCCESS" }
                val deviceResult = externalDevicesRepository.getDeviceByUuid(syncData.deviceId)

                if (deviceResult.isFailure) {
                    Logger.w(TAG) { "CANNOT FIND THE GIVEN DEVICE " }
                    throw InvalidReceiverIdException()
                }

                val currentDeviceInfo = localDeviceProvider.readDeviceInfo.first()
                val outgoingData = BLESyncData.BLEAdvertiseResponse(
                    nonce = syncData.nonce,
                    receiverID = syncData.deviceId,
                    senderID = currentDeviceInfo.deviceId
                )
                val syncWrite = protoBuf.encodeToByteArray(outgoingData)
                peripheral.write(syncCharacteristics, syncWrite)
                send(Resource.Success(BLEDeviceSyncEvent.AdvertisingResponseSend))
                Logger.i(TAG) { "DEVICE INFO WRITE" }
            } catch (_: TimeoutCancellationException) {
                Logger.w(TAG) { "FAILED TO CONNECT TO THE DEVICE TIMEOUT OCCURRED" }
            } catch (e: Exception) {
                Logger.e(TAG, e) { "SOME EXCEPTION" }
            }
            awaitClose {
                Logger.i(TAG) { "PERIPHERAL CONNECTION CLOSED" }
                peripheral.close()
            }
        }.flowOn(Dispatchers.IO)
    }

    private class MissingSyncCharacteristicsException : Exception("Missing advertisement data")
    private class InvalidReceiverIdException : Exception("Invalid receiver id provided")
    private class InvalidAcknowledgementException(reason: BLESyncACKFailedReason) :
        Exception("Invalid Acknowledgement :${reason.name}")

    private class SyncFlagMissingException : Exception("No sync flag found in the read response")
}