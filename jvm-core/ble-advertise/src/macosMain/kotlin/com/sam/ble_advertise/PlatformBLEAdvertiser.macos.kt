package com.sam.ble_advertise

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.NSLogWriter
import co.touchlab.kermit.NoTagFormatter
import co.touchlab.kermit.Severity
import com.sam.ble_advertise.models.BLECharacteristicsModel
import com.sam.ble_advertise.models.GATTAdvertiseConfig
import kotlinx.cinterop.*
import platform.CoreBluetooth.*
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.Foundation.NSError
import platform.darwin.NSUInteger
import kotlin.properties.Delegates


private object MacOsLogWriter : LoggerConfig {

    override val minSeverity: Severity
        get() = Severity.Info

    override val logWriterList: List<LogWriter>
        get() = listOf(NSLogWriter(NoTagFormatter))

}

actual class PlatformBLEAdvertiser : KNativeBLEAdvertiser {

    private val _logger = Logger(MacOsLogWriter, "NATIVE_MACOS_BLE_ADVERTISE")

    // manages a list of service
    private val services = mutableListOf<CBMutableService>()

    private var currentService: CBMutableService? = null

    // managers characteristics associated with a service
    private val characteristicsMap = mutableMapOf<String, CBMutableCharacteristic>()

    // list of subscribed clients
    private val subscribedCentrals = mutableMapOf<String, Set<CBCentral>>()

    private var _advertisementStatus: Int by Delegates.observable(-1) { _, oldValue, newValue ->
        _logger.d { "ADVERTISEMENT STATUS CHANGE FROM :$oldValue $newValue" }
    }

    // callbacks
    private var _callback: BLEMacosAdvertiserCallback? = null

    private val delegate = object : NSObject(), CBPeripheralManagerDelegateProtocol {

        override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
            _logger.d { "PERIPHERAL STATE: ${peripheral.bluetoothStateAsString()}" }
            when (peripheral.state) {
                CBPeripheralManagerStatePoweredOff -> {
                    _advertisementStatus = 1
                    _callback?.onServiceStatusChanged(_advertisementStatus)
                }

                CBPeripheralManagerStatePoweredOn -> {
                    // add the services
                    val lastService = services.removeLastOrNull() ?: return
                    _logger.d { "ADDING SERVICE UUID: ${lastService.UUID}" }
                    peripheral.addService(lastService)
                }

                else -> {}
            }
        }

        override fun peripheralManager(
            peripheral: CBPeripheralManager,
            didAddService: CBService,
            error: NSError?
        ) {
            val serviceUuid = didAddService.UUID.UUIDString
            if (error != null) {
                _logger.e { "ATTACH SERVICE FAILED MESSAGE:${error.description}" }
                _callback?.onServiceAdded(serviceUuid, false, error.code.toInt())
                return
            }

            val errorCode = error?.code?.toInt() ?: 0
            _logger.d { "SERVICE ADDED: UUID:$serviceUuid SUCCESSFULLY" }
            _callback?.onServiceAdded(serviceUuid, true, errorCode)

            // Again loop over to add the next one
            services.removeLastOrNull()?.let(peripheral::addService)
        }


        override fun peripheralManagerDidStartAdvertising(
            peripheral: CBPeripheralManager,
            error: NSError?
        ) {
            if (error == null) {
                _logger.d { "ADVERTISEMENT STARTED" }
                _advertisementStatus = 2
            } else {
                _logger.e { "FAILED TO START ADVERTISEMENTS REASON: ${error.localizedDescription}" }
                _advertisementStatus = 3
            }
            _callback?.onServiceStatusChanged(_advertisementStatus)
        }

        override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveReadRequest: CBATTRequest) {
            val charUuid = didReceiveReadRequest.characteristic.UUID.UUIDString
            val serviceUuid = didReceiveReadRequest.characteristic.service?.UUID?.UUIDString ?: ""
            val deviceAddress = didReceiveReadRequest.central.identifier.UUIDString

            _logger.d { "INCOMING READ REQUEST FOR CHARACTERISTICS: $charUuid SERVICE :$serviceUuid DEVICE:$deviceAddress" }

            val response = _callback?.onReadCharacteristics(deviceAddress, serviceUuid, charUuid, 0)
            if (response != null) {
                val bytes = response.encodeToByteArray()
                val data = bytes.toNSData()
                didReceiveReadRequest.value = data
                _logger.d { "READ REQUEST FOR CHARACTERISTICS: $charUuid SERVICE :$serviceUuid RESPONDING READ SUCCESS" }
                peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorSuccess)
            } else {
                _logger.d { "READ REQUEST FOR CHARACTERISTICS: $charUuid SERVICE :$serviceUuid RESPONDING READ FAILED" }
                peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorAttributeNotFound)
            }
        }


        override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveWriteRequests: List<*>) {
            val requests = didReceiveWriteRequests.filterIsInstance<CBATTRequest>()
            requests.forEach { req ->
                val charUuid = req.characteristic.UUID.UUIDString
                val serviceUuid = req.characteristic.service?.UUID?.UUIDString ?: ""
                val deviceAddress = req.central.identifier.UUIDString

                val value = req.value?.toByteArray() ?: byteArrayOf()

                _logger.d { "INCOMING WRITE REQUEST FOR CHARACTERISTICS: $charUuid SERVICE :$serviceUuid DEVICE:$deviceAddress" }

                val status = _callback?.onWriteCharacteristics(deviceAddress, serviceUuid, charUuid, value, true) ?: 0
                if (status == 0) {
                    _logger.d { "WRITE REQUEST FOR CHARACTERISTICS: $charUuid SERVICE :$serviceUuid RESPONDING SUCCESS" }
                    peripheral.respondToRequest(req, CBATTErrorSuccess)
                } else {
                    _logger.d { "WRITE REQUEST FOR CHARACTERISTICS: $charUuid SERVICE :$serviceUuid RESPONDING FAILED" }
                    peripheral.respondToRequest(req, status.toLong())
                }
            }
        }

        @ObjCSignatureOverride
        override fun peripheralManager(
            peripheral: CBPeripheralManager,
            central: CBCentral,
            didSubscribeToCharacteristic: CBCharacteristic
        ) {
            val charUuid = didSubscribeToCharacteristic.UUID.UUIDString
            _logger.d { "CENTRAL ${central.identifier.UUIDString} SUBSCRIBED TO CHARACTERISTICS $charUuid" }
            // saving it to a map of characteristics uuid to subscribed clients
            val oldEntries = subscribedCentrals[charUuid] ?: emptySet()
            subscribedCentrals[charUuid] = oldEntries + central
        }

        @ObjCSignatureOverride
        override fun peripheralManager(
            peripheral: CBPeripheralManager,
            central: CBCentral,
            didUnsubscribeFromCharacteristic: CBCharacteristic
        ) {
            val charUuid = didUnsubscribeFromCharacteristic.UUID.UUIDString
            _logger.d { "CENTRAL ${central.identifier.UUIDString} UNSUBSCRIBED FROM CHARACTERISTICS $charUuid" }
            val oldEntries = subscribedCentrals[charUuid] ?: emptySet()
            subscribedCentrals[charUuid] = oldEntries.filterNot { it.identifier == central.identifier() }.toSet()
        }
    }

    private val _manager = CBPeripheralManager(delegate, dispatch_get_main_queue())

    actual override fun getStatusInt(): Int = _advertisementStatus

    actual override suspend fun start(config: GATTAdvertiseConfig) {
        val adData: Map<Any?, Any?> = mutableMapOf<Any?, Any?>().apply {
            // add service data uuid keys
            put(CBAdvertisementDataServiceUUIDsKey, services.map { it.UUID })
            if (config.serviceData.isNotEmpty()) {
                // add config data
                put(CBAdvertisementDataLocalNameKey, config.serviceData)
            }
        }
        _logger.i { "STARTING ADVERTISEMENTS" }
        _manager.startAdvertising(adData)
    }

    actual override fun stop() {
        _logger.i { "STOPING ADVERTISEMENTS" }
        _manager.stopAdvertising()
        _manager.removeAllServices()
        _advertisementStatus = 1
        _callback?.onServiceStatusChanged(_advertisementStatus)
    }

    actual override fun addService(serviceUuid: String) {
        _logger.d { "ADDING SERVICE UUID:$serviceUuid" }
        val cbService = CBMutableService(type = CBUUID.UUIDWithString(serviceUuid), primary = true)
        services.add(cbService)
        currentService = cbService
    }

    actual override fun addCharacteristic(characteristic: BLECharacteristicsModel) {
        val cbCharacteristic = CBMutableCharacteristic(
            type = CBUUID.UUIDWithString(characteristic.characteristicUuid),
            properties = characteristic.toCBProperties,
            value = null,
            permissions = characteristic.toCBPermissions,
        )

        characteristicsMap[characteristic.characteristicUuid] = cbCharacteristic

        val service = currentService ?: run {
            _logger.w { "UNABLE TO SET CHARACTERISTICS NO SERVICES BEING SET" }
            return
        }

        _logger.d { "ADDING CHARACTERISTICS UUID:${characteristic.characteristicUuid}" }
        val existing = service.characteristics?.filterIsInstance<CBCharacteristic>() ?: emptyList()
        service.setCharacteristics(existing + cbCharacteristic)

        val updatedList = service.characteristics()?.filterIsInstance<CBCharacteristic>() ?: emptyList()
        _logger.d { "NEW SET OF CHARACTERISTICS :${updatedList.joinToString(",") { it.UUID.toString() }}" }
    }

    actual override fun addDescriptor(characteristicUuid: String, descriptorUuid: String) {
        val cbDescriptor = CBMutableDescriptor(
            type = CBUUID.UUIDWithString(descriptorUuid),
            value = null,
        )

        val characteristic = characteristicsMap.getOrElse(characteristicUuid) { null } ?: run {
            _logger.w { "UNABLE TO SET DESCRIPTOR , MAKE SURE CREATE CHARACTERISTICS FIRST" }
            return
        }
        _logger.d { "ADDING DESCRIPTOR UUID :$descriptorUuid" }
        val existing = characteristic.descriptors?.filterIsInstance<CBDescriptor>() ?: emptyList()
        characteristic.setDescriptors(existing + cbDescriptor)

        val updatedList = characteristic.descriptors()?.filterIsInstance<CBDescriptor>() ?: emptyList()
        _logger.d { "NEW SET OF DESCRIPTOR UUIDS :${updatedList.joinToString(",") { it.UUID.toString() }}" }
    }

    actual override fun sendNotification(deviceAddress: String, characteristicUuid: String, value: ByteArray): Boolean {
        val characteristic = characteristicsMap[characteristicUuid] ?: return false
        val data = value.toNSData()

        val centrals = subscribedCentrals.getOrElse(characteristicUuid) { emptySet() }
        val clients = centrals.filter { it.identifier.UUIDString == deviceAddress }

        _logger.w { "SENDING NOTIFICATION TO CLIENTS :${clients.size}" }

        return _manager.updateValue(
            value = data,
            forCharacteristic = characteristic,
            onSubscribedCentrals = clients,
        )
    }

    actual override fun registerForCallbacks(
        onServiceAdded: (serviceUuid: String, success: Boolean, errorCode: Int) -> Unit,
        onServiceStatusChanged: (status: Int) -> Unit,
        onReadCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, status: Int) -> String,
        onWriteCharacteristics: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, value: ByteArray, respond: Boolean) -> Int,
        onReadDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, status: Int) -> String,
        onWriteDescriptor: (deviceAddress: String, serviceUuid: String, characteristicUuid: String, descriptorUuid: String, value: ByteArray, respond: Boolean) -> Int,
        onIndicationResult: (deviceAddress: String, characteristicUuid: String, success: Boolean, status: Int, errorCode: Int) -> Unit
    ) {
        _callback = object : BLEMacosAdvertiserCallback {
            override fun onServiceAdded(serviceUuid: String, success: Boolean, errorCode: Int) {
                onServiceAdded(serviceUuid, success, errorCode)
            }

            override fun onServiceStatusChanged(status: Int) {
                onServiceStatusChanged(status)
            }

            override fun onReadCharacteristics(
                deviceAddress: String,
                serviceUuid: String,
                characteristicUuid: String,
                status: Int
            ): String = onReadCharacteristics(deviceAddress, serviceUuid, characteristicUuid, status)

            override fun onWriteCharacteristics(
                deviceAddress: String,
                serviceUuid: String,
                characteristicUuid: String,
                value: ByteArray,
                respond: Boolean
            ): Int = onWriteCharacteristics(deviceAddress, serviceUuid, characteristicUuid, value, respond)

            override fun onReadDescriptor(
                deviceAddress: String,
                serviceUuid: String,
                characteristicUuid: String,
                descriptorUuid: String,
                status: Int
            ): String {
                TODO("Not yet implemented")
            }

            override fun onWriteDescriptor(
                deviceAddress: String,
                serviceUuid: String,
                characteristicUuid: String,
                descriptorUuid: String,
                value: ByteArray,
                respond: Boolean
            ): Int {
                TODO("Not yet implemented")
            }

            override fun onIndicationResult(
                deviceAddress: String,
                characteristicUuid: String,
                success: Boolean,
                status: Int,
                errorCode: Int
            ) {
                TODO("Not yet implemented")
            }

        }
    }

    actual override fun onDestroy() {
        _logger.w { "CLEAN UP CODE CALLED" }
        stop()
        services.clear()
        characteristicsMap.clear()
        subscribedCentrals.clear()
    }
}
