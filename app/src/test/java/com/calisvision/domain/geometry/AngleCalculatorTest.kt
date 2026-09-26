package com.calisvision.domain.geometry

import com.calisvision.domain.TestPoses
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark as L
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan

class AngleCalculatorTest {
    private val front = Vec2(1f, 0f)

    @Test
    fun straightLineIs180WithZeroDeviation() {
        val a = Landmark(0.5f, 0.9f)
        val b = Landmark(0.5f, 0.5f)
        val c = Landmark(0.5f, 0.1f)
        assertEquals(180f, AngleCalculator.angle(a, b, c, 1f), 0.01f)
        assertEquals(0f, AngleCalculator.signedDeviation(a, b, c, 1f, front), 0.01f)
    }

    @Test
    fun rightAngleIs90() {
        val angle = AngleCalculator.angle(Landmark(0.2f, 0.5f), Landmark(0.5f, 0.5f), Landmark(0.5f, 0.2f), 1f)
        assertEquals(90f, angle, 0.01f)
    }

    @Test
    fun aspectRatioScalesX() {
        val a = Landmark(0.25f, 0.25f)
        val b = Landmark(0.5f, 0.5f)
        val c = Landmark(0.75f, 0.25f)
        assertEquals(90f, AngleCalculator.angle(a, b, c, 1f), 0.01f)
        val aspect = 9f / 16f
        val expected = Math.toDegrees(2.0 * atan(aspect.toDouble())).toFloat()
        assertEquals(expected, AngleCalculator.angle(a, b, c, aspect), 0.01f)
    }

    @Test
    fun bananaHipTowardFrontIsPositive() {
        val pose = TestPoses.handstand(hipDx = 0.06f)
        val dev = deviation(pose, L.LEFT_SHOULDER, L.LEFT_HIP, L.LEFT_ANKLE)
        assertTrue("dev=$dev", dev > 0f)
        val theta = AngleCalculator.signedAngle(pose.getValue(L.LEFT_SHOULDER), pose.getValue(L.LEFT_HIP), pose.getValue(L.LEFT_ANKLE), 1f, frontSign = 1)
        assertEquals(180f + dev, theta, 0.01f)
    }

    @Test
    fun pikeHipTowardBackIsNegative() {
        val dev = deviation(TestPoses.handstand(hipDx = -0.06f), L.LEFT_SHOULDER, L.LEFT_HIP, L.LEFT_ANKLE)
        assertTrue("dev=$dev", dev < 0f)
    }

    /**
     * Shoulder vertex (wrist-shoulder-hip), face toward +x:
     * + = shoulder pushed toward the face past the wrist–hip line (shoulders leaning over the hands),
     * − = shoulder behind that line, i.e. the arm sits in front of the torso line (closed shoulders).
     */
    @Test
    fun shoulderVertexSign() {
        fun dev(shoulderDx: Float) = deviation(TestPoses.handstand(shoulderDx = shoulderDx), L.LEFT_WRIST, L.LEFT_SHOULDER, L.LEFT_HIP)
        assertTrue(dev(0.04f) > 0f)
        assertTrue(dev(-0.04f) < 0f)
        assertEquals(0f, dev(0f), 0.01f)
    }

    private fun deviation(pose: Map<Int, Landmark>, a: Int, b: Int, c: Int): Float {
        val nose = pose.getValue(L.NOSE)
        val ear = pose.getValue(L.LEFT_EAR)
        val front = Vec2(nose.x - ear.x, nose.y - ear.y)
        return AngleCalculator.signedDeviation(pose.getValue(a), pose.getValue(b), pose.getValue(c), 1f, front)
    }
}
