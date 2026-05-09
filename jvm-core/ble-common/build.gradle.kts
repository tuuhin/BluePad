plugins {
    kotlin("jvm")
}

group = "com.sam.ble_common"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(libs.kermit)
}
