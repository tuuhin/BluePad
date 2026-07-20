package com.sam.bluepad.domain.bluetooth

fun interface BTEnableRequestProvider {

    suspend fun invoke(): Result<Unit>
}
