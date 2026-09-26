package com.calisvision.fixtures

import com.calisvision.domain.analysis.AlignmentGate
import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.analysis.HoldSegmentDetector
import com.calisvision.domain.geometry.SideSelector
import com.calisvision.domain.knowledge.HandstandKnowledge
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** AC-4 on the hold-out fixture; skipped until `fixtures/wall_handstand_holdout.json` exists. */
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

        val theta = AngleTimeline.build(fixture.frames, exercise, orientation!!, fixture.aspect)
            .smoothed()
            .frames
            .filter { it.sampleIndex in hold!! }
            .map { it.angles[HandstandKnowledge.ALIGNMENT] }
        val result = AlignmentGate.evaluate(theta)
        println(result)
        assertTrue(result.reason, result.passed)
    }
}
