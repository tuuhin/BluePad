package com.sam.bluepad.repository.exceptions

import kotlin.uuid.Uuid

internal class InvalidExternalDeviceIdException(private val uuid: Uuid) :
    Exception("Cannot found any device by the given id :$uuid")

