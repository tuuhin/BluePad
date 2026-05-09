plugins {
    kotlin("jvm")
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":jvm-core:ble-common"))
}
