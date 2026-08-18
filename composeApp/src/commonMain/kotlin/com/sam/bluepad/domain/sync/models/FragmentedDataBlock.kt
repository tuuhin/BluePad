package com.sam.bluepad.domain.sync.models

data class FragmentedDataBlock(
    val seqNumber: UInt,
    val payload: String
)
