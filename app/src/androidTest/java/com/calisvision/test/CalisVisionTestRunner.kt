package com.calisvision.test

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class CalisVisionTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TestApp::class.java.name, context)
}
