package com.sam.bluepad.data.ble.exceptions

sealed class BLEConnectionException(override val message: String) : IllegalStateException(message) {


    class WrongLifecycleRoutineException :
        BLEConnectionException("Invalid Lifecycle state, the internal coroutine was cancelled")

    class InvalidCharacteristicsHandlerException :
        BLEConnectionException("Operation on invalid characteristics please report this")
}
