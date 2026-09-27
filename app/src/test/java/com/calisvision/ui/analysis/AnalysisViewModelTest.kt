package com.calisvision.ui.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalysisViewModelTest {

    @Test
    fun estimateNeedsFiveSamplesThenExtrapolatesThePace() {
        assertNull(estimateRemainingSeconds(elapsedMs = 4_000, done = 4, total = 100))
        assertEquals(95, estimateRemainingSeconds(elapsedMs = 5_000, done = 5, total = 100))
        assertEquals("rounds up", 1, estimateRemainingSeconds(elapsedMs = 1_000, done = 99, total = 100))
        assertEquals("pace measured after the baseline", 2, estimateRemainingSeconds(elapsedMs = 2_000, done = 20, total = 30, baseline = 10))
        assertNull("no pace yet", estimateRemainingSeconds(elapsedMs = 0, done = 10, total = 20, baseline = 10))
        assertEquals(0, estimateRemainingSeconds(elapsedMs = 3_000, done = 30, total = 30))
    }
}
