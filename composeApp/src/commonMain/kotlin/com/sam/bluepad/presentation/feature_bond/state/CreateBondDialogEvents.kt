package com.sam.bluepad.presentation.feature_bond.state

sealed interface CreateBondDialogEvents {
    data object OnRequestBondForDevice : CreateBondDialogEvents
    data object OnCancelBondForDevice : CreateBondDialogEvents
    data object OnAcceptConfirmPin : CreateBondDialogEvents
}
