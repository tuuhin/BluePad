package com.sam.bluepad.testing.annotations

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@Target(allowedTargets = [AnnotationTarget.CLASS])
@Retention(value = AnnotationRetention.BINARY)
@RunWith(AndroidJUnit4::class)
actual annotation class RunWithPlatform
