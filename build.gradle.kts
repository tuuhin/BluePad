plugins {
	alias(libs.plugins.androidApplication) apply false
	alias(libs.plugins.androidMultiplatformLibrary) apply false
	alias(libs.plugins.composeHotReload) apply false
	alias(libs.plugins.composeMultiplatform) apply false
	alias(libs.plugins.composeCompiler) apply false
	alias(libs.plugins.kotlinMultiplatform) apply false
	alias(libs.plugins.ksp) apply false
	alias(libs.plugins.androidx.room) apply false
	alias(libs.plugins.kotlinx.serialization) apply false
	alias(libs.plugins.build.konfig) apply false
}