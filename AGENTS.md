# AGENTS.md - BluePad Project Context

## Project Overview

BluePad is a cross-platform (Android and Windows) offline-first app for sketching and idea-syncing built with
**Kotlin Multiplatform (KMP)**. It enables secure, proximity-based syncing of sketches between trusted devices
using Bluetooth Low Energy (BLE), prioritizing privacy, user intent, and data ownership over cloud reliance.

### Key Technologies

- **UI:** Compose Multiplatform, Material 3, Adaptive Layouts, Navigation 3, Compose Toast.
- **Concurrency:** Kotlin Coroutines & Flow.
- **Dependency Injection:** Koin.
- **Persistence:** Room (SQL) and AndroidX DataStore.
- **Communication:** BLE (Kable for scanning, custom JVM JNI for Windows advertising), Moko Permissions.
- **Serialization:** Kotlinx Serialization (Protobuf).
- **IO:** Kotlinx IO for multiplatform stream handling.
- **Security:** Kotlin Crypto (SHA-2, Secure Random) and Cryptography Core for secure communication.
- **Native Access:** Nucleus (KDroidFilter) for JNI management and decorated windows on Desktop.
- **Utils:** Kotlinx DateTime, Kotlinx Immutable Collections, BuildKonfig.
- **Logging:** Kermit.

---

## Architecture and Structure

The project follows **Clean Architecture** principles and is organized into a modular KMP structure:

- **`:composeApp`**: The primary module containing shared business logic, domain models, and the shared UI.
    - `commonMain`: Shared code (Data, Domain, Presentation).
    - `androidMain`: Android-specific implementations (e.g., Room database builders).
    - `jvmMain`: Desktop-specific implementations and resource handling.
- **`:androidApp`**: The entry point for the Android application.
- **`:jvm-core`**: Contains JVM-specific modules for Bluetooth functionality:
    - `ble-advertise`: Handles BLE advertisement on Windows.
    - `ble-common`: Shared BLE utilities for JVM.
- **`cpp/windows`**: C++ source code for Windows-specific Bluetooth functionality via JNI.

### Directory Mapping

- `composeApp/src/commonMain/kotlin/com/sam/bluepad/domain`: Core business logic, entities, and repository interfaces.
- `composeApp/src/commonMain/kotlin/com/sam/bluepad/data`: Implementation of repositories, local data sources (
  Room/DataStore), and sync logic.
- `composeApp/src/commonMain/kotlin/com/sam/bluepad/presentation`: Feature-based UI (Devices, Settings, Sketches, Sync).
- `composeApp/src/commonMain/kotlin/com/sam/bluepad/di`: Koin dependency injection modules.

---

## Building and Running

### Prerequisites

- **JDK:** 22 or higher.
- **OS:** Windows 10/11 (BLE hardware support required for device syncing).
- **C++ Build Tools:** Visual Studio 2022 (Community or higher) with the "Desktop development with C++" workload installed.
- **Windows SDK:** Version 10.0.26100.0 or higher.
- **CMake:** Installed and added to System PATH.
- **IDE:** IntelliJ IDEA or Android Studio with the Kotlin Multiplatform plugin.

### Key Commands

- **Android:**
    - Build & Install: `./gradlew :androidApp:installDebug`
    - Run Tests: `./gradlew :composeApp:testDebugUnitTest`
- **Desktop (JVM):**
    - Run App: `./gradlew :composeApp:run`
    - Package MSI (Windows): `./gradlew :composeApp:packageMsi`
- **General:**
    - Clean Project: `./gradlew clean`
    - Refresh Dependencies: `./gradlew --refresh-dependencies`

---

## Development Conventions

### Coding Standards

- **Clean Architecture:** Strictly separate Domain (pure Kotlin), Data (implementations), and Presentation (Compose).
- **Feature-Based Presentation:** Group UI components, ViewModels, and state by feature within the `presentation` layer.
- **KMP Patterns:** Use `expect`/`actual` for platform-specific capabilities (e.g., Database builders, BLE providers).
- **Dependency Injection:** All dependencies should be provided via Koin modules defined in
  `composeApp/src/commonMain/kotlin/com/sam/bluepad/di`.

### Resources

- All shared resources (images, icons, strings) are managed in `composeApp/src/commonMain/composeResources`.
- Access resources using the generated `Res` class (e.g., `Res.drawable.ic_add`).

### State Management

- Use `StateFlow` and `collectAsStateWithLifecycle` for UI state.
- Leverage `AppCommonViewModel` for globally shared state like Bluetooth status.
