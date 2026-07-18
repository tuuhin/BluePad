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
import com.sam.bluepad.data.ble.delegate.BLEConnectorSyncHandlerDelegate
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLESyncConnectionManager
import com.sam.bluepad.domain.ble.ResourcesSyncDataEvents
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.exceptions.BLENotSupportedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.exceptions.InvalidServiceOrCharacteristicsException
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.utils.Resource
import com.sam.bt_common.isBTActive
import com.sam.bt_common.isLEConnectionAvailable
import com.sam.bt_common.platform.PlatformBTInfoProvider
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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

    private val _receiverInfo = ConcurrentHashMap<String, ExternalDeviceModel>()

    override fun discoverAndConnect(timeout: Duration): Flow<ResourcesSyncDataEvents> =
        flow<ResourcesSyncDataEvents> {
            if (!PlatformBTInfoProvider.isBTActive()) {
                emit(Resource.Error(BluetoothNotEnabledException()))
                return@flow
            }

            if (!PlatformBTInfoProvider.isLEConnectionAvailable()) {
                emit(Resource.Error(BLENotSupportedException()))
                return@flow
            }

            try {
                emit(Resource.Success(ConnectorSyncEvent.DiscoveryStarted))
                Logger.i(tag = TAG) { "SCANNING FOR DEVICES STATED" }
                val firstAdvertisement = withTimeout(timeout) {
                    _scanner.advertisements
                        // an extra delay for the hardware to cool down
                        .onEach { delay(100.milliseconds) }
                        .first()
                }
                Logger.i(tag = TAG) { "SCAN RESULT FOUND" }
                val identifier = firstAdvertisement.identifier.toString()
                emit(Resource.Success(ConnectorSyncEvent.DeviceFound(identifier)))
                // deals with the connection
                val connectionFlow = handleConnection(firstAdvertisement)
                emitAll(connectionFlow.map { Resource.Success(it) })
            } catch (_: TimeoutCancellationException) {
                Logger.w(tag = TAG) { "SCAN TIMEOUT ADVERTISEMENT NOT FOUND FOR TIMEOUT: $timeout" }
                emit(Resource.Success(ConnectorSyncEvent.DeviceScanTimeout))
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Logger.d(tag = TAG) { "CONNECTION JOB CANCELLED" }
                    throw e
                }
                emit(Resource.Error(e))
                Logger.e(tag = TAG, throwable = e) { "Some exception" }
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
                Logger.e(tag = TAG, throwable = exp) { "EXCEPTION HAPPENED" }
            }
            onServicesDiscovered {
                Logger.d(tag = TAG) { "SERVICE DISCOVERED SERVICE" }
            }
        }

        val deviceAddress = peripheral.identifier.toString()

        return channelFlow {
            try {
                withTimeout(connectionTimeout) {
                    peripheral.connect()
                    peripheral.maximumWriteValueLengthForType(WriteType.WithoutResponse)
                    send(ConnectorSyncEvent.ConnectionSuccess)
                    Logger.i(tag = TAG) { "PERIPHERAL CONNECTION ESTABLISHED" }
                }
            } catch (_: TimeoutCancellationException) {
                send(ConnectorSyncEvent.DeviceScanTimeout)
                Logger.i(tag = TAG) { "FAILED TO CONNECT TO THE DEVICE TIMEOUT OCCURRED" }
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
                        onObserveBytes = { bytes ->
                            val result = delegate.handleSyncDataNotification(
                                characteristicId = BLEConstants.SYNC_DATA_CHARACTERISTICS_ID,
                                value = bytes,
                                onWriteBytes = { dataBytes ->
                                    peripheral.writeToCharacteristics(syncDataCharacteristics, dataBytes)
                                },
                                onToggleNotification = { _, enable ->
                                    if (enable) Unit
                                    else cancel()
                                    true
                                },
                                onEvent = { event -> trySend(event) },
                                onReadDevice = { _receiverInfo[deviceAddress] },
                            )

                            if (result.isFailure) {
                                close(result.exceptionOrNull())
                                return@observeNotifications
                            }
                        },
                        onToggleNotification = { _, _ -> false },
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
                                else -> {}
                            }
                            // no marker present thus directly marking its as true
                            true
                        },
                        onObserveBytes = { bytes ->
                            val event = delegate.handleHandshakeNotification(
                                value = bytes,
                                onHandshakeSuccess = {
                                    if (this.isActive) {
                                        _receiverInfo[deviceAddress]?.let { device ->
                                            trySend(ConnectorSyncEvent.HandshakeSuccess(device))
                                        }
                                        Logger.d(tag = TAG) { "HANDSHAKE IS DONE WE CAN CANCEL IT NOW" }
                                        cancel()
                                    }
                                    return@handleHandshakeNotification true
                                },
                            )
                            if (event.isFailure) {
                                val error = event.exceptionOrNull()
                                trySend(ConnectorSyncEvent.HandshakeFailed(error?.message))
                                return@observeNotifications
                            }
                        },
                    )
                }

                val bytes = peripheral.read(handshakeCharacteristics)


                val deviceResult = delegate.handleHandshakeRead(
                    deviceAddress = deviceAddress,
                    value = bytes,
                    deviceInfo = localDeviceProvider.readDeviceInfo.first(),
                    savedDevices = externalDevicesRepository::getDeviceByUuid,
                    onReadSuccess = {
                        // start the handshake notification reader
                        Logger.d(tag = TAG) { "HANDSHAKE SUCCESSFULLY WAITING FOR APPROVAL " }
                        handshakeJob.start()
                    },
                )
                deviceResult.fold(
                    onSuccess = { device -> _receiverInfo[deviceAddress] = device },
                    onFailure = { err ->
                        trySend(ConnectorSyncEvent.HandshakeFailed(err.message))
                        close(err)
                        return@channelFlow
                    },
                )

                // now join the handshake notification one
                handshakeJob.join()
                syncDataJob.join()

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(tag = TAG, throwable = e) { "UNKNOWN EXCEPTION MESSAGE:$e" }
            } finally {
                try {
                    Logger.i(tag = TAG) { "DISCONNECTING THE PERIPHERAL" }
                    peripheral.disconnect()
                } catch (_: CancellationException) {
                    Logger.w(tag = TAG) { "FAILED TO DISCONNECT THE PERIPHERAL" }
                }
            }
            awaitClose {
                Logger.i(tag = TAG) { "PERIPHERAL CONNECTION CLOSED" }
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
                ?: throw InvalidServiceOrCharacteristicsException(false, BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID)
        }

    private val List<DiscoveredCharacteristic>.syncDataCharacteristics: Result<DiscoveredCharacteristic>
        get() = runCatching {
            find { it.characteristicUuid == BLEConstants.SYNC_DATA_CHARACTERISTICS_ID }
                ?: throw InvalidServiceOrCharacteristicsException(false, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID)
        }

    private suspend fun Peripheral.writeToCharacteristics(
        characteristic: DiscoveredCharacteristic,
        bytes: ByteArray
    ) = try {
        write(characteristic, bytes)
        true
    } catch (_: NotConnectedException) {
        false
    }


    private suspend fun DiscoveredCharacteristic.observeNotifications(
        peripheral: Peripheral,
        onToggleNotification: suspend (Uuid, Boolean) -> Boolean,
        onObserveBytes: suspend (ByteArray) -> Unit,
    ) = coroutineScope {

        val cccDescriptor = descriptors.find { it.descriptorUuid == BLEConstants.CCC_DESCRIPTOR }
            ?: return@coroutineScope

        peripheral.observe(
            characteristic = this@observeNotifications,
            onSubscription = {
                val descriptorValue = peripheral.read(cccDescriptor)
                Logger.d(tag = TAG) { "READY TO OBSERVE NOTIFICATIONS" }
                // mostly ccc will be enabled here
                delegate.onEnabledDisabledCCCDescriptor(
                    address = peripheral.identifier.toString(),
                    characteristicId = this@observeNotifications.characteristicUuid,
                    bytes = descriptorValue,
                    onWriteBytes = { bytes -> peripheral.writeToCharacteristics(this@observeNotifications, bytes) },
                    onToggleNotification = onToggleNotification,
                )
            },
        )
            .onStart { Logger.d(tag = TAG) { "CHARACTERISTICS:${characteristicUuid} NOTIFICATION OBSERVER STARTED" } }
            .onCompletion {
                val descriptorValue = peripheral.read(cccDescriptor)
                Logger.d(tag = TAG) { "CHARACTERISTICS:${characteristicUuid} NOTIFICATION OBSERVER STOPPED" }
                // mostly ccc will be disabled here
                delegate.onEnabledDisabledCCCDescriptor(
                    address = peripheral.toString(),
                    characteristicId = this@observeNotifications.characteristicUuid,
                    bytes = descriptorValue,
                    onWriteBytes = { bytes -> peripheral.writeToCharacteristics(this@observeNotifications, bytes) },
                    onToggleNotification = onToggleNotification,
                )
            }
            .collect(onObserveBytes)
    }
}
