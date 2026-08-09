package com.sam.bluepad.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class InstrumentTestRunner : AndroidJUnitRunner() {


    override fun newApplication(classLoader: ClassLoader?, className: String?, context: Context?): Application? {
        return super.newApplication(classLoader, BluePadTestApplication::class.java.name, context)
    }
}
