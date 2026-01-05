package com.sam.ble_common

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

 object NativeLibraryLoader {

	private const val NATIVE_FOLDER = "native"

	private val operatingSystem: String
		get() {
			val os = System.getProperty("os.name").lowercase()
			return when {
				os.contains("windows") -> "windows"
				os.contains("linux") -> "linux"
				os.contains("mac") -> "macos"
				else -> throw UnsupportedOperationException("Unsupported operating system: $os")
			}
		}

	private val architecture: String
		get() {
			val arch = System.getProperty("os.arch").lowercase()
			return when {
				arch.contains("amd64") || arch.contains("x86_64") -> "x64"
				arch.contains("aarch64") -> "aarch64"
				arch.contains("x86") || arch.contains("i386") || arch.contains("i686") -> "x86"
				else -> throw UnsupportedOperationException("Unsupported architecture: $arch")
			}
		}

	private fun getLibraryExtension(os: String): String {
		return when (os) {
			"windows" -> ".dll"
			"linux" -> ".so"
			"macos" -> ".dylib"
			else -> throw UnsupportedOperationException("Unsupported OS: $os")
		}
	}

	@Suppress("UnsafeDynamicallyLoadedCode")
	@JvmStatic
	@kotlin.jvm.Throws(IOException::class)
	fun loadLibrary(libraryName: String?) {
		val libExtension = getLibraryExtension(operatingSystem)

		// Construct the path to the native library inside the JAR
		val fileName = "$libraryName$libExtension"
		val resourcePath = "$NATIVE_FOLDER/$operatingSystem-$architecture/$fileName"
		// Create a temporary directory for extracting the native library
		val tempDir = createTempDirectory()

		// Extract and load the library
		val libraryPath = extractAndGetLibraryPath(resourcePath, tempDir, fileName)
		System.load(libraryPath.toAbsolutePath().toString())
	}


	@Throws(IOException::class)
	private fun createTempDirectory(): Path {
		val tempDirName = "blepad-nativelibs-" + UUID.randomUUID()
		val tempDir = Files.createTempDirectory(tempDirName)
		tempDir.toFile().deleteOnExit()
		return tempDir
	}

	@Throws(IOException::class)
	private fun extractAndGetLibraryPath(resourcePath: String, tempDir: Path, fileName: String)
			: Path {
		val targetPath = tempDir.resolve(fileName)

		this::class.java.classLoader.getResourceAsStream(resourcePath)?.use { stream ->
			Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING)
		} ?: error("FILE PATH NOT FOUND")

		return targetPath
	}
}