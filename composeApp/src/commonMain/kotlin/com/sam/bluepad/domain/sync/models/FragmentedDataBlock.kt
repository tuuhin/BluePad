package com.sam.bluepad.domain.sync.models

data class FragmentedDataBlock(
    val seqNumber: Int,
    val payload: String
)