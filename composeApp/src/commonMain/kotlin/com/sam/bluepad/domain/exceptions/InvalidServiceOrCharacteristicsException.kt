package com.sam.bluepad.domain.exceptions

import kotlin.uuid.Uuid

class InvalidServiceOrCharacteristicsException(isService: Boolean, uuid: Uuid) :
    Exception("Missing required ${if (isService) "SERVICE" else "CHARACTERISTICS"} UUID : ${uuid.toHexString()}")
