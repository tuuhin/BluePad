package com.sam.bluepad.domain.sync.exceptions

import kotlin.uuid.Uuid

sealed class OutgoingDataException(override val message: String) : Exception(message) {

    class SessionNotFoundException(sessionId: Uuid) :
        OutgoingDataException("No active sync session found for ID: $sessionId.")

    class WindowLimitReachedException :
        OutgoingDataException("Cannot provide next chunk: window limit reached, waiting for acknowledgements.")

    class NoMoreChunksException : OutgoingDataException("Data queue is empty; no more chunks available to process.")
}
