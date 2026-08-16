package com.sam.bluepad.device_info

internal object DeviceCommons {

    val manufactureMap = buildMap {
        put(0, "Ericsson Technology Licensing")
        put(2, "Intel Corp.")
        put(10, "Qualcomm / Atheros")
        put(13, "Texas Instruments Inc.")
        put(15, "Broadcom Corporation");
        put(18, "Infineon Technologies AG")
        put(29, "Qualcomm Technologies International")
        put(47, "Marvell Technology Group Ltd.")
        put(76, "Apple Inc.");
        put(113, "Realtek Semiconductor Corp.")
        put(224, "Nordic Semiconductor ASA")
        put(6, "Microsoft")
    }

    val lmpVersions = buildMap {
        put(0, "Bluetooth 1.0b")
        put(1, "Bluetooth 1.1")
        put(2, "Bluetooth 1.2")
        put(3, "Bluetooth 2.0")
        put(4, "Bluetooth 2.1");
        put(5, "Bluetooth 3.0")
        put(6, "Bluetooth 4.0")
        put(7, "Bluetooth 4.1")
        put(8, "Bluetooth 4.2");
        put(9, "Bluetooth 5.0")
        put(10, "Bluetooth 5.1")
        put(11, "Bluetooth 5.2")
        put(12, "Bluetooth 5.3")
        put(13, "Bluetooth 5.4")
    }
}
