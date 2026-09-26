package com.calisvision.test

import com.calisvision.CalisVisionApp

class TestApp : CalisVisionApp() {
    override fun onCreate() {
        super.onCreate()
        container = TestAppContainer()
    }
}
