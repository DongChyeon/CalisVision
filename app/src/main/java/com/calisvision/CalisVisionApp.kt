package com.calisvision

import android.app.Application
import com.calisvision.data.AppContainer
import com.calisvision.data.DefaultAppContainer
import java.io.File

open class CalisVisionApp : Application() {

    /** Replaceable so instrumented tests can swap in fakes (see TestApp). */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        cleanOrphanedSessions()
    }

    /** Every session dir under cacheDir/analysis is an orphan at process start. */
    private fun cleanOrphanedSessions() {
        File(cacheDir, ANALYSIS_DIR).listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { it.deleteRecursively() }
    }

    companion object {
        const val ANALYSIS_DIR = "analysis"
    }
}
