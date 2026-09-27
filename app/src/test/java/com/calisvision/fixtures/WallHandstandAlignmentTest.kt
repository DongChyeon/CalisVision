package com.calisvision.fixtures

import com.calisvision.domain.analysis.AlignmentGate
import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.analysis.HoldSegmentDetector
import com.calisvision.domain.analysis.ReferenceHoldGate
import com.calisvision.domain.geometry.SideSelector
import com.calisvision.domain.knowledge.HandstandKnowledge
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * AC-4 (ADR-0008) on the hold-out fixture: the reference hold must produce no in-hold alignment fault and a mean θ in
 * [175,185]. The original strict metric ([AlignmentGate], θ ∈ [175,185] ≥ 95%) is printed as information only.
 */
class WallHandstandAlignmentTest {

    @Test
    fun holdSegmentIsAligned() {
        val fixture = PoseFixture.load("/fixtures/wall_handstand_holdout.json")
        assumeTrue("hold-out fixture missing", fixture != null)
        fixture!!
        val exercise = HandstandKnowledge.exercise

        val hold = HoldSegmentDetector.detect(fixture.frames, fixture.aspect)
        assertNotNull("no hold segment", hold)
        val holdFrames = fixture.frames.filter { it.sampleIndex in hold!! }
        println("hold segment $hold: ${holdFrames.size} samples")

        val orientation = SideSelector.select(holdFrames, exercise.keyJoints, fixture.aspect)
        assertNotNull("orientation undecidable", orientation)

        val timeline = AngleTimeline.build(fixture.frames, exercise, orientation!!, fixture.aspect).smoothed()
        val strict = AlignmentGate.evaluate(
            timeline.frames.filter { it.sampleIndex in hold!! }.map { it.angles[HandstandKnowledge.ALIGNMENT] }
        )
        println("strict metric (information only): $strict")

        val result = ReferenceHoldGate.evaluate(timeline, hold!!, exercise.rules.single { it.id == HandstandKnowledge.ALIGNMENT })
        println(result)
        assertTrue(result.reasons.toString(), result.passed)
    }
}
