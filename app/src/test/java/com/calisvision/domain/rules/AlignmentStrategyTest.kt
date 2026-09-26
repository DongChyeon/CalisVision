package com.calisvision.domain.rules

import com.calisvision.domain.TestPoses
import com.calisvision.domain.geometry.AngleCalculator
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark as L
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AlignmentStrategyTest {

    private fun alignment(pose: Map<Int, Landmark>) = AlignmentStrategy.deviation(
        pose.getValue(L.LEFT_WRIST), pose.getValue(L.LEFT_SHOULDER), pose.getValue(L.LEFT_HIP), pose.getValue(L.LEFT_ANKLE),
        aspect = 1f, frontSign = 1,
    )

    @Test
    fun straightBodyIsZero() {
        assertEquals(0f, alignment(TestPoses.handstand()), 0.01f)
    }

    @Test
    fun picksHipWhenHipDeviatesMore() {
        val pose = TestPoses.handstand(hipDx = 0.08f, shoulderDx = -0.01f)
        val hip = AngleCalculator.signedDeviation(pose.getValue(L.LEFT_SHOULDER), pose.getValue(L.LEFT_HIP), pose.getValue(L.LEFT_ANKLE), 1f, 1)
        val shoulder = AngleCalculator.signedDeviation(pose.getValue(L.LEFT_WRIST), pose.getValue(L.LEFT_SHOULDER), pose.getValue(L.LEFT_HIP), 1f, 1)
        assertTrue(abs(hip) > abs(shoulder))
        assertEquals(hip, alignment(pose), 1e-4f)
        assertTrue(alignment(pose) > 0f)
    }

    @Test
    fun picksShoulderWhenShoulderDeviatesMore() {
        val pose = TestPoses.handstand(shoulderDx = -0.06f)
        val shoulder = AngleCalculator.signedDeviation(pose.getValue(L.LEFT_WRIST), pose.getValue(L.LEFT_SHOULDER), pose.getValue(L.LEFT_HIP), 1f, 1)
        val hip = AngleCalculator.signedDeviation(pose.getValue(L.LEFT_SHOULDER), pose.getValue(L.LEFT_HIP), pose.getValue(L.LEFT_ANKLE), 1f, 1)
        assertTrue(abs(shoulder) > abs(hip))
        assertEquals(shoulder, alignment(pose), 1e-4f)
        assertTrue(alignment(pose) < 0f)
    }
}
