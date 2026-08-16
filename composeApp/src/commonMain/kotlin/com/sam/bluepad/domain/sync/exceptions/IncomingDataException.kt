package com.sam.bluepad.domain.sync.exceptions

sealed class IncomingDataException(override val message: String) : Exception(message) {

    class EmptyPayloadException : IncomingDataException("No more payload left to work with")
}
