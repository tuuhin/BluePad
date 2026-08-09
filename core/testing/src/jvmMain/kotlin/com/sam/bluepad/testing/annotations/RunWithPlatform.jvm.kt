package com.sam.bluepad.testing.annotations

import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@RunWith(JUnit4::class)
actual annotation class RunWithPlatform
