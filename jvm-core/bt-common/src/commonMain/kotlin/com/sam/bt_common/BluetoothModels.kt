package com.sam.bt_common

import kotlin.jvm.JvmStatic

class Service(
    val uuid: String,
    val data: ByteArray = byteArrayOf(),
    val characteristics: List<Characteristic> = emptyList()
) {
    class Builder(private val uuid: String) {
        private val characteristics = mutableListOf<Characteristic>()
        private var data = byteArrayOf()

        fun addCharacteristic(characteristic: Characteristic): Builder {
            characteristics.add(characteristic)
            return this
        }

        fun addCharacteristics(characteristics: List<Characteristic>): Builder {
            this.characteristics.addAll(characteristics)
            return this
        }

        fun setData(value: ByteArray): Builder {
            data = value
            return this
        }

        fun build() = Service(uuid, data, characteristics.toList())
    }

    companion object {
        @JvmStatic
        fun builder(uuid: String) = Builder(uuid)
    }
}

data class Characteristic(
    val uuid: String,
    val descriptors: List<Descriptor>,
    val canRead: Boolean,
    val canWriteRequest: Boolean,
    val canWriteCommand: Boolean,
    val canNotify: Boolean,
    val canIndicate: Boolean
) {
    class Builder(private val uuid: String) {
        private val descriptors = mutableListOf<Descriptor>()
        private var canRead = false
        private var canWriteRequest = false
        private var canWriteCommand = false
        private var canNotify = false
        private var canIndicate = false

        fun addDescriptor(descriptor: Descriptor): Builder {
            descriptors.add(descriptor)
            return this
        }

        fun canRead(value: Boolean): Builder {
            canRead = value
            return this
        }

        fun canWriteRequest(value: Boolean): Builder {
            canWriteRequest = value
            return this
        }

        fun canWriteCommand(value: Boolean): Builder {
            canWriteCommand = value
            return this
        }

        fun canNotify(value: Boolean): Builder {
            canNotify = value
            return this
        }

        fun canIndicate(value: Boolean): Builder {
            canIndicate = value
            return this
        }

        fun build() = Characteristic(
            uuid,
            descriptors.toList(),
            canRead,
            canWriteRequest,
            canWriteCommand,
            canNotify,
            canIndicate,
        )
    }

    companion object {
        fun builder(uuid: String) = Builder(uuid)
    }
}

data class Descriptor(val uuid: String)

data class BluetoothAddress(val address: String)

enum class BluetoothAddressType {
    PUBLIC,
    RANDOM
}

data class BluetoothUUID(val uuid: String)
