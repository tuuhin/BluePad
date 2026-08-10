package com.sam.bluepad.crypto.domain

import com.sam.bluepad.model.crypto.EncryptionResult

internal interface ICryptoKeysFileProvider {


    suspend fun readKeyResult(): EncryptionResult
    suspend fun saveKeyResult(keyResult: EncryptionResult)

    suspend fun deleteSavedKey()
}
