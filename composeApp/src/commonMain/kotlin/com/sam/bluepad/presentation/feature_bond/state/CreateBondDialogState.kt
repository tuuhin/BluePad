package com.sam.bluepad.presentation.feature_bond.state

data class CreateBondDialogState(
    val error: String? = null,
    val confirmPin: String? = null,
    val canShowConfirmPinInDialog: Boolean = false,
    val isPrimaryActionEnabled: Boolean = true,
)
