package com.calisvision.ui.result

import android.net.Uri
import com.calisvision.data.AnalysisSessionStore
import com.calisvision.data.InMemoryThresholdRepository
import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.Exercise
import com.calisvision.video.AnalysisProgress
import com.calisvision.video.VideoAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModelTest {

    private class CountingAnalyzer : VideoAnalyzer {
        var calls = 0

        override fun analyze(uri: Uri, exercise: Exercise): Flow<AnalysisProgress> {
            calls++
            return emptyFlow()
        }
    }

    private val analyzer = CountingAnalyzer()
    private val sessions = AnalysisSessionStore().apply { put(SyntheticResult.session()) }
    private val repository = InMemoryThresholdRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ResultViewModel(sessions["synthetic"]!!, repository.thresholds)

    /** AC-7: one step is one sample, and image path, skeleton and angles all point at that sample. */
    @Test
    fun stepMovesExactlyOneSampleAndKeepsFrameSkeletonAnglesInSync() {
        val vm = viewModel()
        val timeline = vm.result.timeline
        vm.seekTo(0)
        for (expected in 1 until SyntheticResult.SAMPLES) {
            vm.step(1)
            val state = vm.state.value
            assertEquals(expected, state.sampleIndex)
            assertEquals(expected * 100L, state.displayTimeMs)
            assertEquals(timeline.frames.single { it.sampleIndex == expected }.angles, state.angles)
            assertEquals("$expected.jpg", vm.framePath(expected).name)
            assertEquals(expected, vm.overlay(expected).pose?.sampleIndex)
        }
        vm.step(1)
        assertEquals("clamped at the last sample", SyntheticResult.SAMPLES - 1, vm.state.value.sampleIndex)
        vm.step(-1)
        assertEquals(SyntheticResult.SAMPLES - 2, vm.state.value.sampleIndex)
    }

    /** AC-7: on the scrubber track, moving by one cell width always moves exactly one sample. */
    @Test
    fun scrubberCellWidthIsOneSample() {
        val width = 997f
        val n = SyntheticResult.SAMPLES
        val cell = width / n
        for (i in 0 until n) {
            assertEquals(i, sampleAt((i + 0.5f) * cell, width, n))
        }
        assertEquals(0, sampleAt(-5f, width, n))
        assertEquals(n - 1, sampleAt(width + 5f, width, n))
    }

    /** AC-9: a threshold change re-evaluates faults from the stored angles; the analyzer is never called. */
    @Test
    fun thresholdChangeUpdatesFaultsWithoutReanalysis() = runTest {
        val vm = viewModel()
        fun holdFaultIds() = vm.state.value.holdFaults.map { it.faultId }
        assertEquals(listOf(HandstandKnowledge.BANANA.id, HandstandKnowledge.PIKE.id), holdFaultIds())

        repository.set(HandstandKnowledge.ALIGNMENT, AngleThreshold.Deviation(maxExtensionDeg = 20f, maxFlexionDeg = 10f))
        assertEquals(listOf(HandstandKnowledge.PIKE.id), holdFaultIds())

        vm.seekTo(SyntheticResult.BANANA.first)
        assertFalse(HandstandKnowledge.ALIGNMENT in vm.state.value.brokenRules)

        repository.reset()
        assertEquals(listOf(HandstandKnowledge.BANANA.id, HandstandKnowledge.PIKE.id), holdFaultIds())
        assertTrue(HandstandKnowledge.ALIGNMENT in vm.state.value.brokenRules)
        assertEquals(0, analyzer.calls)
    }

    /** User decision 1: the pike during the kick-up stays on the timeline but out of the list. */
    @Test
    fun onlyFaultsInsideHoldAreListed() {
        val state = viewModel().state.value
        val outside = state.faults.single { it.range == SyntheticResult.OUTSIDE_PIKE }
        assertFalse(outside in state.holdFaults)
        val merged = state.holdFaults.single { it.range == SyntheticResult.MERGED_PIKE }
        assertEquals(setOf(HandstandKnowledge.ALIGNMENT, HandstandKnowledge.HIP), merged.angles.keys)
    }

    /** A fault that starts before the hold is listed with its range and peak angles clipped to the hold; its band keeps the full range. */
    @Test
    fun listedFaultsAreClippedToTheHold() {
        val base = SyntheticResult.session()
        val hold = 22..43
        // 바나나 등 peaks at 200° only before the hold (20..21); inside it stays at 195°.
        val timeline = AngleTimeline(base.result.timeline.frames.map { f ->
            if (f.sampleIndex in 20..21) f.copy(angles = f.angles + (HandstandKnowledge.ALIGNMENT to 200f)) else f
        })
        val session = base.copy(result = base.result.copy(holdSegment = hold, timeline = timeline))
        val vm = ResultViewModel(session, repository.thresholds)
        val state = vm.state.value

        val banana = state.holdFaults.single { it.faultId == HandstandKnowledge.BANANA.id }
        assertEquals(22..29, banana.range)
        assertEquals(mapOf(HandstandKnowledge.ALIGNMENT to 195f), banana.angles)
        assertEquals(15f, banana.peakDeviation, 0.01f)
        val pike = state.holdFaults.single { it.faultId == HandstandKnowledge.PIKE.id }
        assertEquals(40..43, pike.range)

        val band = state.bands.single { it.fault.faultId == HandstandKnowledge.BANANA.id }
        assertEquals(SyntheticResult.BANANA, band.fault.range)
        assertEquals(banana, band.listed)
        assertEquals(null, state.bands.single { it.fault.range == SyntheticResult.OUTSIDE_PIKE }.listed)

        vm.selectFault(banana)
        assertEquals(banana, vm.state.value.selectedFault)
        assertEquals(22, vm.state.value.sampleIndex)
    }

    @Test
    fun selectingAFaultOpensItAtItsFirstSample() {
        val vm = viewModel()
        val banana = vm.state.value.holdFaults.first { it.faultId == HandstandKnowledge.BANANA.id }
        vm.selectFault(banana)
        assertEquals(banana, vm.state.value.selectedFault)
        assertEquals(SyntheticResult.BANANA.first, vm.state.value.sampleIndex)
        vm.dismissFault()
        assertEquals(null, vm.state.value.selectedFault)
    }
}
