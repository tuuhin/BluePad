plugins {
    `kotlin-dsl`
}

group = "com.sam.bluepad.plugins"
version = "1.0.0"

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.nucleus.nna.gradlePlugin)
}

gradlePlugin {
    plugins {
        create("ktNativeJna") {
            id = "com.sam.bluepad.nucleus.nna.cmakeExt"
            implementationClass = "com.sam.bluepad.plugins.KTNativeJNAPlugin"
        }
        create("ktDistributableExt") {
            id = "com.sam.bluepad.nucleus.buildExt"
            implementationClass = "com.sam.bluepad.plugins.KTNucleusPackagingExtPlugin"
        }
        create("ktUPXCompression") {
            id = "com.sam.bluepad.nucleus.upxCompression"
            implementationClass = "com.sam.bluepad.plugins.KTUPXCompressionPlugin"
        }
    }
}
