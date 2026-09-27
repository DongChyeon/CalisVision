package com.calisvision.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calisvision.data.InMemoryThresholdRepository
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.test.SyntheticResult
import com.calisvision.ui.result.ResultScreen
import com.calisvision.ui.result.ResultViewModel
import com.calisvision.ui.theme.CalisVisionTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** AC-8: tapping a listed fault opens a sheet with its name and correction hint; merged faults show both rules. */
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
}
