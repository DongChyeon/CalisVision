package com.calisvision.domain.analysis

import com.calisvision.domain.knowledge.HandstandKnowledge as H
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.RuleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaultEvaluatorTest {

    private fun timeline(size: Int, series: Map<RuleId, Map<Int, Float>>) = AngleTimeline(
        List(size) { i ->
            AngleFrame(i + 100, (i + 100) * 100L, H.rules.associate { it.id to (series[it.id]?.get(i) ?: 180f) })
        }
    )

    private fun span(range: IntRange, value: Float) = range.associateWith { value }

    private fun evaluate(t: AngleTimeline) = FaultEvaluator.evaluate(t, H.exercise)

    @Test
    fun needsAtLeastThreeConsecutiveSamples() {
        assertTrue(evaluate(timeline(10, mapOf(H.ALIGNMENT to span(2..3, 195f)))).isEmpty())
        val segments = evaluate(timeline(10, mapOf(H.ALIGNMENT to span(2..4, 195f))))
        assertEquals(1, segments.size)
        assertEquals(H.BANANA, segments[0].fault)
        assertEquals(102..104, segments[0].range)
        assertEquals(15f, segments[0].peakDeviation, 0.01f)
    }

    @Test
    fun nullBreaksARun() {
        val t = timeline(6, mapOf(H.ALIGNMENT to span(0..5, 195f)))
        val withGap = AngleTimeline(t.frames.mapIndexed { i, f -> if (i == 2) f.copy(angles = f.angles.mapValues { null }) else f })
        assertEquals(listOf(103..105), evaluate(withGap).map { it.range })
    }

    @Test
    fun faultNameDependsOnSign() {
        assertEquals(H.PIKE, evaluate(timeline(5, mapOf(H.ALIGNMENT to span(0..4, 165f)))).single().fault)
        assertEquals(H.ANTERIOR_TILT, evaluate(timeline(5, mapOf(H.HIP to span(0..4, 200f)))).single().fault)
        assertEquals(H.PIKE, evaluate(timeline(5, mapOf(H.HIP to span(0..4, 160f)))).single().fault)
    }

    @Test
    fun rangeRulesFlagBelowMin() {
        val segment = evaluate(timeline(5, mapOf(H.ELBOW_LOCK to span(0..2, 160f)))).single()
        assertEquals(H.BENT_ELBOW, segment.fault)
        assertEquals(-10f, segment.peakDeviation, 0.01f)
        assertEquals(mapOf(H.ELBOW_LOCK to 160f), segment.angles)
        assertEquals(H.CLOSED_SHOULDER, evaluate(timeline(5, mapOf(H.SHOULDER_OPEN to span(1..3, 150f)))).single().fault)
    }

    @Test
    fun pikeFromTwoRulesMergesWithinOneSampleGap() {
        val t = timeline(12, mapOf(H.ALIGNMENT to span(0..3, 165f), H.HIP to span(5..8, 160f)))
        val segment = evaluate(t).single()
        assertEquals(H.PIKE, segment.fault)
        assertEquals(100..108, segment.range)
        assertEquals(mapOf(H.ALIGNMENT to 165f, H.HIP to 160f), segment.angles)
        assertEquals(-20f, segment.peakDeviation, 0.01f)
    }

    @Test
    fun pikeWithTwoSampleGapStaysSeparate() {
        val t = timeline(12, mapOf(H.ALIGNMENT to span(0..3, 165f), H.HIP to span(6..9, 160f)))
        assertEquals(listOf(100..103, 106..109), evaluate(t).map { it.range })
    }

    @Test
    fun bananaAndAnteriorTiltNeverMerge() {
        val t = timeline(6, mapOf(H.ALIGNMENT to span(0..4, 195f), H.HIP to span(0..4, 200f)))
        val faults = evaluate(t).map { it.fault }.toSet()
        assertEquals(setOf(H.BANANA, H.ANTERIOR_TILT), faults)
    }

    @Test
    fun thresholdOverridesReapplyWithoutRebuilding() {
        val t = timeline(5, mapOf(H.ALIGNMENT to span(0..4, 195f)))
        val loose = mapOf(H.ALIGNMENT to AngleThreshold.Deviation(maxExtensionDeg = 20f, maxFlexionDeg = 10f))
        assertTrue(FaultEvaluator.evaluate(t, H.exercise, loose).isEmpty())
    }
}
