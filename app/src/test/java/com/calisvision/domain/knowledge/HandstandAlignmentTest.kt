package com.calisvision.domain.knowledge

import com.calisvision.domain.TestPoses
import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.analysis.FaultEvaluator
import com.calisvision.domain.geometry.AngleCalculator
import com.calisvision.domain.geometry.BodyOrientation
import com.calisvision.domain.knowledge.HandstandKnowledge as H
import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.Joint
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark as L
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HandstandAlignmentTest {
    private val orientation = BodyOrientation(BodySide.LEFT, 1)

    private fun alignment(pose: Map<Int, Landmark>): Float {
        val frames = listOf(TestPoses.frame(0, pose))
        return AngleTimeline.build(frames, H.exercise, orientation, aspect = 1f).frames[0].angles.getValue(H.ALIGNMENT)!!
    }

    private fun dev(pose: Map<Int, Landmark>, a: Int, b: Int, c: Int) =
        AngleCalculator.signedDeviation(pose.getValue(a), pose.getValue(b), pose.getValue(c), 1f, frontSign = 1)

    @Test
    fun ruleIsWristHipAnkle() {
        assertEquals(listOf(Joint.WRIST, Joint.HIP, Joint.ANKLE), H.rules.single { it.id == H.ALIGNMENT }.joints)
    }

    @Test
    fun straightIs180AndHipSideSetsSign() {
        assertEquals(180f, alignment(TestPoses.handstand()), 0.01f)
        val banana = TestPoses.handstand(hipDx = 0.05f)
        assertEquals(180f + dev(banana, L.LEFT_WRIST, L.LEFT_HIP, L.LEFT_ANKLE), alignment(banana), 1e-3f)
        assertTrue(alignment(banana) > 190f)
        assertTrue(alignment(TestPoses.handstand(hipDx = -0.05f)) < 170f)
    }

    @Test
    fun bananaWithClosedShouldersIsBananaNotPike() {
        val pose = TestPoses.handstand(hipDx = 0.05f, shoulderDx = -0.04f)
        val atShoulder = dev(pose, L.LEFT_WRIST, L.LEFT_SHOULDER, L.LEFT_HIP)
        val atHip = dev(pose, L.LEFT_SHOULDER, L.LEFT_HIP, L.LEFT_ANKLE)
        assertTrue("old max-|dev| picked the closed shoulder", atShoulder < 0f && abs(atShoulder) > abs(atHip))

        val frames = (0 until 5).map { TestPoses.frame(it, pose) }
        val timeline = AngleTimeline.build(frames, H.exercise, orientation, aspect = 1f)
        val alignmentRule = H.rules.filter { it.id == H.ALIGNMENT }
        assertEquals(H.BANANA, FaultEvaluator.evaluate(timeline, alignmentRule).single().fault)
        assertFalse(FaultEvaluator.evaluate(timeline, H.exercise).any { it.fault == H.PIKE })
    }
}
