package com.calisvision.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calisvision.CalisVisionApp
import com.calisvision.R
import com.calisvision.test.FakeAnalyzer
import com.calisvision.test.FakePickVideo
import com.calisvision.test.SyntheticResult
import com.calisvision.test.TestAppContainer
import com.calisvision.video.AnalysisProgress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** AC-6 (home → guide → picker → progress → result) and AC-11 (guide is shown before the picker opens). */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun str(id: Int) = context.getString(id)

    @Test
    fun homeToGuideToPickerToResult() {
        val picker = FakePickVideo()
        val analyzer = FakeAnalyzer { listOf(AnalysisProgress.Processing(1, 2), AnalysisProgress.Completed(SyntheticResult.session().result)) }
        (context as CalisVisionApp).container = TestAppContainer(analyzer = analyzer, pickVideo = picker)

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText(str(R.string.home_start)).performClick()

            compose.onNodeWithText(str(R.string.guide_headline)).assertExists()
            assertEquals("picker must not open before the guide", 0, picker.launches)

            compose.onNodeWithText(str(R.string.guide_pick_video)).performClick()
            assertEquals(1, picker.launches)

            compose.waitUntil(5_000) { compose.onAllNodesWithText(str(R.string.result_title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(str(R.string.fault_list_title)).assertExists()
        }
    }

    @Test
    fun pickerCancelStaysOnGuide() {
        val picker = FakePickVideo(result = null)
        (context as CalisVisionApp).container = TestAppContainer(pickVideo = picker)

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText(str(R.string.home_start)).performClick()
            compose.onNodeWithText(str(R.string.guide_pick_video)).performClick()
            assertEquals(1, picker.launches)
            compose.onNodeWithText(str(R.string.guide_headline)).assertExists()
        }
    }
}
