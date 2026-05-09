package com.sam.bluepad.domain.crypto

import com.sam.bluepad.domain.crypto.models.KeyEncryptionResult

interface KeyFileManager {

    suspend fun readKeyResult(): KeyEncryptionResult
    suspend fun saveKeyResult(keyResult: KeyEncryptionResult)

    suspend fun deleteSavedKey()
}
