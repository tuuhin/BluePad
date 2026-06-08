#-------------------------------------------------------------------------
# Core Java and JVM Foundation Rules
#-------------------------------------------------------------------------

# Keep foundational JVM attributes required for stack traces and debugging
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Deprecated,AnnotationDefault,*Annotation*

# Prevent ProGuard from breaking standard Java library resource lookups
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**

#-------------------------------------------------------------------------
# Kotlin Runtime and Coroutines
#-------------------------------------------------------------------------

# Keep Kotlin metadata and intrinsics
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# Keep Kotlin Coroutines service providers and volatile fields
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-dontwarn kotlinx.coroutines.**

#-------------------------------------------------------------------------
# JetBrains Compose Desktop Framework
#-------------------------------------------------------------------------

# Keep all Compose Multiplatform structural components
-keep class org.jetbrains.compose.** { *; }
-keep class androidx.compose.** { *; }

# Prevent optimization from breaking internal Compose state management
-keepclassmembers class * extends androidx.compose.runtime.RecomposeScope { *; }
-keepclassmembers class * extends androidx.compose.runtime.Applier { *; }

# Keep Compose Skia native graphics rendering layer intact
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }
-dontwarn org.jetbrains.skia.**
-dontwarn org.jetbrains.skiko.**

# Keep Material / Material 3 components and icon vector logic
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material3.adaptive.** { *; }

#-------------------------------------------------------------------------
# Room Database Multiplatform
#-------------------------------------------------------------------------

# Keep Room structural components and generated implementations
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep @androidx.room.Entity class *
-keep class * { @androidx.room.Dao *; }
-keep class * { @androidx.room.Database *; }

# Keep constructor for RoomDatabase and its generated _Impl classes
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase_Impl {
    <init>(...);
}

# Keep the project's database package
-keep class com.sam.bluepad.data.database.** { *; }

#-------------------------------------------------------------------------
# Koin Dependency Injection
#-------------------------------------------------------------------------

# Keep Koin core and DSL components
-keep class org.koin.** { *; }
-dontwarn org.koin.**

#-------------------------------------------------------------------------
# Kotlinx Serialization & Protobuf
#-------------------------------------------------------------------------

# Keep Companion objects and serializer methods used for reflection/lookup
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    *** serializer(...);
}
-dontwarn kotlinx.serialization.**

#-------------------------------------------------------------------------
# DataStore & Preferences
#-------------------------------------------------------------------------

# Keep DataStore classes to prevent breaking local persistence
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

#-------------------------------------------------------------------------
# Moko Permissions Multiplatform
#-------------------------------------------------------------------------

# Keep permissions controller and state classes
-keep class dev.icerock.moko.permissions.** { *; }
-dontwarn dev.icerock.moko.permissions.**

#-------------------------------------------------------------------------
# Kable (Bluetooth Low Energy) & BLE Scanning
#-------------------------------------------------------------------------

# Keep Kable core and exception classes
-keep class com.juul.kable.** { *; }
-dontwarn com.juul.kable.**

#-------------------------------------------------------------------------
# Jetpack Navigation 3
#-------------------------------------------------------------------------

# Keep Navigation 3 structural components
-keep class org.jetbrains.androidx.navigation3.** { *; }

#-------------------------------------------------------------------------
# KDroidFilter / Nucleus (Native Access & Window Management)
#-------------------------------------------------------------------------

# Keep Nucleus classes for JNI and native library access
-keep class io.github.kdroidfilter.nucleus.** { *; }
-dontwarn io.github.kdroidfilter.nucleus.**

#-------------------------------------------------------------------------
# Cryptography (Whyoleg & Kotlin Crypto)
#-------------------------------------------------------------------------

# Keep cryptography providers and core logic
-keep class dev.whyoleg.cryptography.** { *; }
-dontwarn dev.whyoleg.cryptography.**

-keep class org.kotlincrypto.** { *; }
-dontwarn org.kotlincrypto.**

#-------------------------------------------------------------------------
# Kermit (Logging)
#-------------------------------------------------------------------------

# Keep Kermit loggers and configurations
-keep class co.touchlab.kermit.** { *; }

#-------------------------------------------------------------------------
# BuildKonfig Generated Config
#-------------------------------------------------------------------------

# Keep the generated BuildKonfig class to ensure versioning/flags remain accessible
-keep class com.sam.bluepad.BuildKonfig { *; }

#-------------------------------------------------------------------------
# Project Data Models & Entities
#-------------------------------------------------------------------------

# Prevent removal of domain models and data entities used across the app
-keep class com.sam.bluepad.domain.models.** { *; }
-keep class com.sam.bluepad.data.database.entities.** { *; }
-keep class com.sam.bluepad.data.database.dao.** { *; }
-keep class com.sam.bluepad.data.sync.dto.** { *; }
-keep class com.sam.bluepad.data.sync.models.** { *; }
