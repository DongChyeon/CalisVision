package com.calisvision.test

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @Test
    fun applicationIsTestApp() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertTrue("Expected TestApp but was ${app.javaClass.name}", app is TestApp)
    }
}
