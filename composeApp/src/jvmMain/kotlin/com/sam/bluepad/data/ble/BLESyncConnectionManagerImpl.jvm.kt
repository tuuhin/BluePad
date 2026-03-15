package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.juul.kable.Advertisement
import com.juul.kable.DiscoveredCharacteristic
import com.juul.kable.DiscoveredService
import com.juul.kable.NotConnectedException
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType
import com.juul.kable.logs.Logging
import com.sam.ble_common.BluetoothInfoProvider
import com.sam.bluepad.data.ble.delegate.BLEConnectorSyncHandlerDelegate
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLESyncConnectionManager
import com.sam.bluepad.domain.ble.ResourcesSyncDataEvents
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.exceptions.BLENotSupportedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.exceptions.InvalidServiceOrCharacteristicsException
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private const val TAG = "BLE_SYNC_CONNECTION_MANAGER"

actual class BLESyncConnectionManagerImpl private constructor(
    private val localDeviceProvider: LocalDeviceInfoProvider,
    private val externalDevicesRepository: ExternalDevicesRepository,
    private val delegate: BLEConnectorSyncHandlerDelegate,
) : BLESyncConnectionManager {

    constructor(
        deviceInfoProvider: LocalDeviceInfoProvider,
        externalDevicesRepository: ExternalDevicesRepository,
        protoBuf: ProtoBuf,
        syncOutPayloadManager: OutPayloadManager,
        syncInPayloadManager: InPayloadManager,
    ) : this(
        localDeviceProvider = deviceInfoProvider,
        externalDevicesRepository = externalDevicesRepository,
        delegate = BLEConnectorSyncHandlerDelegate(
            protoBuf = protoBuf,
            outPayloadManager = syncOutPayloadManager,
            inPayloadManager = syncInPayloadManager,
        ),
    )

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
                level = Logging.Level.Data
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
                    peripheral.maximumWriteValueLengthForType(WriteType.WithoutResponse)
                    send(ConnectorSyncEvent.ConnectionSuccess)
                    Logger.i(TAG) { "PERIPHERAL CONNECTION ESTABLISHED" }
                }
            } catch (_: TimeoutCancellationException) {
                send(ConnectorSyncEvent.DeviceScanTimeout)
                Logger.i(TAG) { "FAILED TO CONNECT TO THE DEVICE TIMEOUT OCCURRED" }
                close()
            }

            try {
                // read the required characteristic
                val services = peripheral.services.value ?: emptyList()
                val syncService = services.getSyncService.getOrThrow()

                val handshakeCharacteristics = syncService.characteristics
                    .handshakeCharacteristics.getOrThrow()

                val syncDataCharacteristics = syncService.characteristics
                    .syncDataCharacteristics.getOrThrow()

                // handle sync notification
                val syncDataJob = launch(start = CoroutineStart.LAZY) {
                    syncDataCharacteristics.observeNotifications(
                        peripheral = peripheral,
                        onToggleNotification = { uuid, enable -> },
                        onObserveBytes = { bytes ->
                            delegate.handleSyncDataNotification(
                                characteristicId = BLEConstants.SYNC_DATA_CHARACTERISTICS_ID,
                                value = bytes,
                                onWriteBytes = { dataBytes ->
                                    peripheral.writeToCharacteristics(syncDataCharacteristics, dataBytes)
                                },
                                onToggleNotification = { _, enable ->
                                    if (enable) Unit
                                    else cancel()
                                },
                                onError = { err -> close(err) },
                                onEvent = { event -> trySend(event) },
                            )
                        },
                    )
                }

                // handle the handshake notifications
                val handshakeJob = launch(start = CoroutineStart.LAZY) {
                    handshakeCharacteristics.observeNotifications(
                        peripheral = peripheral,
                        onToggleNotification = { uuid, enable ->
                            when (uuid) {
                                syncDataCharacteristics.characteristicUuid if (enable) -> syncDataJob.start()
                                syncDataCharacteristics.characteristicUuid -> syncDataJob.cancel()
                                handshakeCharacteristics.characteristicUuid if !enable -> cancel()
                            }
                        },
                        onObserveBytes = { bytes ->
                            delegate.handleHandshakeNotification(
                                value = bytes,
                                onHandshakeSuccess = {
                                    if (this.isActive) {
                                        Logger.d(TAG) { "HANDSHAKE IS DONE WE CAN CANCEL IT NOW" }
                                        cancel()
                                    }
                                    return@handleHandshakeNotification true
                                },
                            )
                        },
                    )
                }

                val bytes = peripheral.read(handshakeCharacteristics)
                val deviceResult = delegate.handleHandshakeRead(
                    deviceAddress = peripheral.identifier.toString(),
                    value = bytes,
                    deviceInfo = localDeviceProvider.readDeviceInfo.first(),
                    savedDevices = externalDevicesRepository::getDeviceByUuid,
                    onReadSuccess = {
                        // start the handshake notification reader
                        Logger.d(TAG) { "HANDSHAKE SUCCESSFULLY WAITING FOR APPROVAL " }
                        handshakeJob.start()
                    },
                )

                if (deviceResult.isFailure) {
                    close(deviceResult.exceptionOrNull())
                    return@channelFlow
                }

                trySend(ConnectorSyncEvent.ConnectorDeviceDataResponseSend)
                // now join the handshake notification one
                handshakeJob.join()
                syncDataJob.join()

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(TAG, e) { "UNKNOWN EXCEPTION MESSAGE:$e" }
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

    private val List<DiscoveredService>.getSyncService: Result<DiscoveredService>
        get() = runCatching {
            find { it.serviceUuid == BLEConstants.SYNC_SERVICE_ID }
                ?: throw InvalidServiceOrCharacteristicsException(true, BLEConstants.SYNC_SERVICE_ID)
        }

    private val List<DiscoveredCharacteristic>.handshakeCharacteristics: Result<DiscoveredCharacteristic>
        get() = runCatching {
            find { it.characteristicUuid == BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID }
                ?: throw InvalidServiceOrCharacteristicsException(
                    false,
                    BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID,
                )
        }

    private val List<DiscoveredCharacteristic>.syncDataCharacteristics: Result<DiscoveredCharacteristic>
        get() = runCatching {
            find { it.characteristicUuid == BLEConstants.SYNC_DATA_CHARACTERISTICS_ID }
                ?: throw InvalidServiceOrCharacteristicsException(false, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID)
        }

    private suspend fun Peripheral.writeToCharacteristics(
        characteristic: DiscoveredCharacteristic,
        bytes: ByteArray
    ): Boolean {
        return try {
            write(characteristic, bytes)
            true
        } catch (_: NotConnectedException) {
            false
        }
    }

    private suspend fun DiscoveredCharacteristic.observeNotifications(
        peripheral: Peripheral,
        onToggleNotification: (Uuid, Boolean) -> Unit,
        onObserveBytes: suspend (ByteArray) -> Unit,
    ) = coroutineScope {

        val cccDescriptor = descriptors.find { it.descriptorUuid == BLEConstants.CCC_DESCRIPTOR }
            ?: return@coroutineScope

        peripheral
            .observe(
                characteristic = this@observeNotifications,
                onSubscription = {
                    Logger.d(TAG) { "READY TO OBSERVE NOTIFICATIONS" }
                    // mostly ccc will be enabled here
                    delegate.onEnabledDisabledCCCDescriptor(
                        address = peripheral.identifier.toString(),
                        characteristicId = this@observeNotifications.characteristicUuid,
                        bytes = peripheral.read(cccDescriptor),
                        onWriteBytes = { bytes -> peripheral.writeToCharacteristics(this@observeNotifications, bytes) },
                        onToggleNotification = onToggleNotification,
                    )
                },
            )
            .onStart { Logger.d(TAG) { "CHARACTERISTICS:${characteristicUuid} NOTIFICATION OBSERVER STARTED" } }
            .onCompletion {
                Logger.d(TAG) { "CHARACTERISTICS:${characteristicUuid} NOTIFICATION OBSERVER STOPPED" }
                // mostly ccc will be disabled here
                delegate.onEnabledDisabledCCCDescriptor(
                    address = peripheral.toString(),
                    characteristicId = this@observeNotifications.characteristicUuid,
                    bytes = peripheral.read(cccDescriptor),
                    onWriteBytes = { bytes -> peripheral.writeToCharacteristics(this@observeNotifications, bytes) },
                    onToggleNotification = onToggleNotification,
                )
            }
            .collect(onObserveBytes)
    }
}
