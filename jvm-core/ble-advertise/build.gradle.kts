plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(22)
}

dependencies {
    implementation(project(":jvm-core:ble-common"))
}
