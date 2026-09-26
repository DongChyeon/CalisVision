package com.calisvision.domain.analysis

import com.calisvision.domain.TestPoses
import com.calisvision.domain.geometry.BodyOrientation
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.domain.model.BodySide
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

    @Test
    fun smoothedKeepsRuleIdsAndSampleIndices() {
        val poses = (0 until 5).map { TestPoses.frame(it, TestPoses.handstand()) }
        val smoothed = AngleTimeline.build(poses, exercise, orientation, aspect = 1f).smoothed()
        assertEquals((0 until 5).toList(), smoothed.frames.map { it.sampleIndex })
        assertEquals(4, smoothed.frames[2].angles.size)
    }
}
