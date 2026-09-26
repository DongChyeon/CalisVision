package com.calisvision.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlignmentGateTest {

    @Test
    fun passesWhenAtLeast95PercentAligned() {
        val series = List(95) { 178f } + List(5) { 190f }
        val result = AlignmentGate.evaluate(series)
        assertTrue(result.reason, result.passed)
        assertEquals(0.95f, result.ratio, 1e-4f)
    }

    @Test
    fun failsWhenHoldShorterThanFiveSeconds() {
        val result = AlignmentGate.evaluate(List(49) { 180f })
        assertFalse(result.passed)
        assertEquals(49, result.holdSamples)
    }

    @Test
    fun nullSamplesCountAsFailures() {
        val result = AlignmentGate.evaluate(List(94) { 180f } + List<Float?>(6) { null })
        assertFalse(result.passed)
        assertEquals(0.94f, result.ratio, 1e-4f)
    }
}
