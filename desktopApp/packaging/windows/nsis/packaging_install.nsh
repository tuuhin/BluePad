!macro customInstall
  SetRegView 64
  WriteRegStr HKCU "${INSTALL_REGISTRY_KEY}" "InstallLocation" "$INSTDIR"

  SetRegView 32
  WriteRegStr HKCU "${INSTALL_REGISTRY_KEY}" "InstallLocation" "$INSTDIR"
!macroend

!macro customUnInstall
  SetRegView 64
  DeleteRegValue HKCU "${INSTALL_REGISTRY_KEY}" "InstallLocation"

  SetRegView 32
  DeleteRegValue HKCU "${INSTALL_REGISTRY_KEY}" "InstallLocation"
!macroend
