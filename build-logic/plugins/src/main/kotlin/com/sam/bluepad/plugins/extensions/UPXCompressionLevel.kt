package com.sam.bluepad.plugins.extensions

enum class UpxCompressionLevel(val flag: String) {
    FAST("-1"),
    BALANCED("-6"),
    HIGH("-9"),
    BEST("--best")
}

enum class UpxStrategy(val flag: String?) {
    DEFAULT(null),
    BRUTE("--brute"),
    ULTRA_BRUTE("--ultra-brute")
}
