package com.sam.bluepad.crypto.domain.exceptions

internal class CryptoMissingKeyException : Exception("Unable to read secret key, it maybe deleted for safety")
