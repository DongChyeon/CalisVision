package com.calisvision.domain.analysis

import com.calisvision.domain.knowledge.HandstandKnowledge as H
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceHoldGateTest {

    private val rule = H.rules.single { it.id == H.ALIGNMENT }

    /** Alignment θ per sample (index = sampleIndex); other rules absent. */
    private fun timeline(values: List<Float?>) = AngleTimeline(
        values.mapIndexed { i, v -> AngleFrame(i, i * 100L, mapOf(H.ALIGNMENT to v)) }
    )

    private fun evaluate(values: List<Float?>, hold: IntRange = values.indices) =
        ReferenceHoldGate.evaluate(timeline(values), hold, rule)

    @Test
    fun passesStraightHoldEvenOutsideStrictBand() {
        // 40% of samples at 188° would fail the strict [175,185]/95% gate but stay within the ±10° fault threshold.
        val result = evaluate(List(60) { 182f } + List(40) { 188f })
        assertTrue(result.reasons.toString(), result.passed)
        assertEquals(100, result.holdSamples)
        assertEquals(184.4f, result.meanTheta!!, 1e-3f)
        assertEquals(188f, result.maxTheta!!, 1e-3f)
        assertTrue(result.holdFaults.isEmpty())
    }

    @Test
    fun failsOnInHoldFault() {
        val result = evaluate(List(50) { 180f } + List(3) { 191f } + List(47) { 180f })
        assertFalse(result.passed)
        assertEquals(H.BANANA, result.holdFaults.single().fault)
        assertEquals(50..52, result.holdFaults.single().range)
    }

    @Test
    fun ignoresFaultOutsideHoldAndClipsToIt() {
        val values = List(10) { 195f } + List(100) { 180f }
        assertTrue(evaluate(values, 10..109).passed)
        // Overlap of 2 samples with the hold is below MIN_SAMPLES: not an in-hold fault.
        assertTrue(evaluate(values, 8..109).passed)
        val clipped = evaluate(values, 5..109).holdFaults.single()
        assertEquals(5..9, clipped.range)
    }

    @Test
    fun failsWhenMeanOutsideRange() {
        val result = evaluate(List(100) { 186f })
        assertFalse(result.passed)
        assertTrue(result.holdFaults.isEmpty())
        assertEquals(1, result.reasons.size)
    }

    @Test
    fun failsWhenMoreThanFivePercentNull() {
        val alternate = { nulls: Int -> List(100) { i -> if (i % 10 == 0 && i / 10 < nulls) null else 180f } }
        assertTrue(evaluate(alternate(5)).passed)
        val result = evaluate(alternate(6))
        assertFalse(result.passed)
        assertEquals(6, result.nullSamples)
    }

    @Test
    fun failsWhenHoldShorterThanFiftySamples() {
        val result = evaluate(List(49) { 180f })
        assertFalse(result.passed)
        assertEquals(49, result.holdSamples)
        assertTrue(evaluate(List(50) { 180f }).passed)
    }
}
