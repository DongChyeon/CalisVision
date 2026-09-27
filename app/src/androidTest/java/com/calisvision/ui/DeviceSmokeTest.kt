package com.calisvision.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.calisvision.CalisVisionApp
import com.calisvision.R
import com.calisvision.test.FakePickVideo
import com.calisvision.test.RequiresVideo
import com.calisvision.test.TestAppContainer
import com.calisvision.test.TestVideos
import com.calisvision.video.DefaultVideoAnalyzer
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Full flow on the device with the REAL analyzer on the tune video (only the picker is faked); saves screenshots of
 * each screen to <external files>/screenshots/ for manual review (`adb pull`).
 */
@RequiresVideo
@RunWith(AndroidJUnit4::class)
class DeviceSmokeTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun str(id: Int) = context.getString(id)

    @Test
    fun realAnalysisReachesResult() {
        val video = File(TestVideos.dir(context), "wall_handstand_tune.mp4")
        assumeTrue("tune video missing: $video", video.exists())
        val app = context as CalisVisionApp
        app.container = TestAppContainer(analyzer = DefaultVideoAnalyzer(context), pickVideo = FakePickVideo(Uri.fromFile(video)))
        val shots = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText(str(R.string.home_start)).assertExists()
            screenshot(shots, "home")
            compose.onNodeWithText(str(R.string.home_start)).performClick()
            compose.onNodeWithText(str(R.string.guide_headline)).assertExists()
            screenshot(shots, "guide")
            compose.onNodeWithText(str(R.string.guide_pick_video)).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText(str(R.string.analysis_title)).fetchSemanticsNodes().isNotEmpty() }
            Thread.sleep(8_000)
            screenshot(shots, "analysis")
            compose.waitUntil(240_000) { compose.onAllNodesWithText(str(R.string.result_title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("timeline").performSemanticsAction(SemanticsActions.SetProgress) { it(75f) }
            compose.waitForIdle()
            Thread.sleep(1_000)
            screenshot(shots, "result")
            compose.onNodeWithTag("result_scroll").performTouchInput { swipeUp() }
            Thread.sleep(500)
            screenshot(shots, "result_scrolled")
            if (compose.onAllNodesWithTag("fault_item").fetchSemanticsNodes().isNotEmpty()) {
                compose.onAllNodesWithTag("fault_item")[0].performClick()
                Thread.sleep(1_000)
                screenshot(shots, "fault_sheet")
            }
        }
    }

    private fun screenshot(dir: File, name: String) {
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        File(dir, "p3_$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
