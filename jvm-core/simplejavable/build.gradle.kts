plugins {
	id("java-library")
	kotlin("jvm")
}

group = "org.simplejavable"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}
dependencies {
	implementation(project(":jvm-core:ble-common"))
}
