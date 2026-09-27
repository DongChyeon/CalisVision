package com.calisvision.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calisvision.data.InMemoryThresholdRepository
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.test.SyntheticResult
import com.calisvision.ui.result.ResultScreen
import com.calisvision.ui.result.ResultViewModel
import com.calisvision.ui.theme.CalisVisionTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AC-8: the timeline draws a band per fault, tapping an in-hold band or a listed fault opens a sheet with its name and
 * correction hint; merged faults show both rules.
 */
@RunWith(AndroidJUnit4::class)
class FaultSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        val viewModel = ResultViewModel(SyntheticResult.session(), InMemoryThresholdRepository().thresholds)
        compose.setContent {
            CalisVisionTheme {
                ResultScreen(viewModel, onBack = {}, onAnotherVideo = {}, onOpenSettings = {})
            }
        }
    }

    @Test
    fun tappingBananaOpensSheetWithHint() {
        compose.onNodeWithText(HandstandKnowledge.BANANA.name).performScrollTo().performClick()
        compose.onNodeWithText(HandstandKnowledge.BANANA.correctionHint).assertIsDisplayed()
        compose.onNodeWithText(HandstandKnowledge.BANANA.description).assertIsDisplayed()
    }

    @Test
    fun mergedPikeShowsBothRuleAngles() {
        compose.onNodeWithText(HandstandKnowledge.PIKE.name).performScrollTo().performClick()
        compose.onNodeWithText(HandstandKnowledge.PIKE.correctionHint).assertIsDisplayed()
        val inSheet = hasAnyAncestor(hasTestTag("fault_sheet"))
        compose.onNode(hasText(HandstandKnowledge.rules[0].name) and inSheet).assertExists()
        compose.onNode(hasText(HandstandKnowledge.rules[1].name) and inSheet).assertExists()
    }

    @Test
    fun timelineBandsRenderAndTappingInHoldBandOpensSheet() {
        val band = hasTestTag("timeline_band")
        val listed = hasTestTag("timeline_band_listed")
        val banana = hasContentDescription(HandstandKnowledge.BANANA.name)
        val pike = hasContentDescription(HandstandKnowledge.PIKE.name)
        // Full bands: 바나나 등 20..29, 파이크 0..4 (outside the hold) and 40..45; only the in-hold ones are listed.
        compose.onAllNodesWithTag("timeline_band").assertCountEquals(3)
        compose.onAllNodes(band and pike).assertCountEquals(2)
        compose.onAllNodesWithTag("timeline_band_listed").assertCountEquals(2)
        compose.onAllNodes(listed and pike).assertCountEquals(1)

        // The outside-hold 파이크 band (0..4, the leftmost) is drawn but not tappable.
        val pikeBands = compose.onAllNodes(band and pike).fetchSemanticsNodes()
        assertTrue(pikeBands[0].boundsInRoot.left < pikeBands[1].boundsInRoot.left)
        compose.onAllNodes(band and pike)[0].performScrollTo().performTouchInput { click(center) }
        compose.onAllNodesWithTag("fault_sheet").assertCountEquals(0)

        compose.onNode(listed and banana).performScrollTo().performTouchInput { click(center) }
        val inSheet = hasAnyAncestor(hasTestTag("fault_sheet"))
        compose.onNode(hasText(HandstandKnowledge.BANANA.name) and inSheet).assertIsDisplayed()
        compose.onNodeWithText(HandstandKnowledge.BANANA.correctionHint).assertIsDisplayed()
    }
}
