package com.calisvision.domain.analysis

import com.calisvision.domain.TestPoses
import com.calisvision.domain.geometry.BodyOrientation
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark
import com.calisvision.domain.rules.PoseFault
import com.calisvision.domain.rules.RuleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AngleTimelineTest {
    private val exercise = HandstandKnowledge.exercise
    private val orientation = BodyOrientation(BodySide.LEFT, 1)

    @Test
    fun everyFrameHasAllFourRuleIdsAndNullWhenMissing() {
        val poses = listOf(
            TestPoses.frame(0, TestPoses.handstand()),
            TestPoses.frame(1, null),
            TestPoses.frame(2, TestPoses.handstand(hipDx = 0.06f)),
        )
        val timeline = AngleTimeline.build(poses, exercise, orientation, aspect = 1f)
        val ruleIds = exercise.rules.map { it.id }.toSet()
        assertEquals(4, ruleIds.size)
        timeline.frames.forEach { assertEquals(ruleIds, it.angles.keys) }

        ruleIds.forEach { assertEquals(180f, timeline.frames[0].angles.getValue(it)!!, 0.01f) }
        assertTrue(timeline.frames[1].angles.values.all { it == null })
        assertTrue(timeline.frames[2].angles.getValue(HandstandKnowledge.ALIGNMENT)!! > 180f)
        assertTrue(timeline.frames[2].angles.getValue(HandstandKnowledge.HIP)!! > 180f)
        assertEquals(listOf(0, 1, 2), timeline.frames.map { it.sampleIndex })
    }

    private fun faults(points: Map<Int, Landmark>, ruleId: RuleId): List<PoseFault> {
        val timeline = AngleTimeline.build((0 until 5).map { TestPoses.frame(it, points) }, exercise, orientation, aspect = 1f)
        return FaultEvaluator.evaluate(timeline, exercise.rules.filter { it.id == ruleId }).map { it.fault }
    }

    private fun theta(points: Map<Int, Landmark>, ruleId: RuleId) =
        AngleTimeline.build(listOf(TestPoses.frame(0, points)), exercise, orientation, aspect = 1f).frames[0].angles.getValue(ruleId)!!

    @Test
    fun elbowFlexionIsBelowAndHyperextensionIsNotBent() {
        // Front is +x; elbow 0.1 from wrist and shoulder, so 0.0132 off-line is a 15° bend.
        fun elbowAt(dx: Float) = TestPoses.handstand() + (PoseLandmark.LEFT_ELBOW to Landmark(0.5f + dx, 0.8f))
        val flexed = elbowAt(-0.0132f)
        val hyper = elbowAt(0.0132f)
        assertEquals(165f, theta(flexed, HandstandKnowledge.ELBOW_LOCK), 0.1f)
        assertEquals(195f, theta(hyper, HandstandKnowledge.ELBOW_LOCK), 0.1f)
        assertEquals(listOf(HandstandKnowledge.BENT_ELBOW), faults(flexed, HandstandKnowledge.ELBOW_LOCK))
        assertTrue(faults(hyper, HandstandKnowledge.ELBOW_LOCK).isEmpty())
    }

    @Test
    fun closedShoulderIsBelowAndOverOpenIsNotClosed() {
        val closed = TestPoses.handstand(shoulderDx = -0.03f)
        val overOpen = TestPoses.handstand(shoulderDx = 0.03f)
        assertTrue(theta(closed, HandstandKnowledge.SHOULDER_OPEN) < 165f)
        assertTrue(theta(overOpen, HandstandKnowledge.SHOULDER_OPEN) > 195f)
        assertEquals(listOf(HandstandKnowledge.CLOSED_SHOULDER), faults(closed, HandstandKnowledge.SHOULDER_OPEN))
        assertTrue(faults(overOpen, HandstandKnowledge.SHOULDER_OPEN).isEmpty())
    }

    @Test
    fun smoothedKeepsRuleIdsAndSampleIndices() {
        val poses = (0 until 5).map { TestPoses.frame(it, TestPoses.handstand()) }
        val smoothed = AngleTimeline.build(poses, exercise, orientation, aspect = 1f).smoothed()
        assertEquals((0 until 5).toList(), smoothed.frames.map { it.sampleIndex })
        assertEquals(4, smoothed.frames[2].angles.size)
    }
}
