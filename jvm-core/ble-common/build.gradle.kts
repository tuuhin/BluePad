plugins {
    kotlin("jvm")
}

group = "com.sam.ble_common"

kotlin {
    jvmToolchain(22)
}

dependencies {
    api(libs.kermit)
}
