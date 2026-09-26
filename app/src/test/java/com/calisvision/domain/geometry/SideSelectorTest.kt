package com.calisvision.domain.geometry

import com.calisvision.domain.TestPoses
import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.Joint
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SideSelectorTest {
    private val keyJoints = listOf(Joint.WRIST, Joint.SHOULDER, Joint.HIP, Joint.ANKLE)

    private fun mirroredFace(points: Map<Int, Landmark>): Map<Int, Landmark> =
        points + (PoseLandmark.NOSE to points.getValue(PoseLandmark.NOSE).copy(x = 0.47f))

    @Test
    fun majorityWinsAndMinorityFramesAreIgnored() {
        val frames = (0 until 5).map { TestPoses.frame(it, TestPoses.handstand()) } +
            (5 until 7).map { TestPoses.frame(it, mirroredFace(TestPoses.handstand(sideVisibility = 0.2f, otherSideVisibility = 0.9f))) }
        assertEquals(BodyOrientation(BodySide.LEFT, 1), SideSelector.select(frames, keyJoints, 1f))
    }

    @Test
    fun rightSideAndNegativeFrontWhenMajority() {
        val frames = (0 until 3).map {
            TestPoses.frame(it, mirroredFace(TestPoses.handstand(sideVisibility = 0.2f, otherSideVisibility = 0.9f)))
        }
        assertEquals(BodyOrientation(BodySide.RIGHT, -1), SideSelector.select(frames, keyJoints, 1f))
    }

    @Test
    fun tieResolvesToLeftAndPositive() {
        val frames = listOf(
            TestPoses.frame(0, TestPoses.handstand()),
            TestPoses.frame(1, mirroredFace(TestPoses.handstand(sideVisibility = 0.2f, otherSideVisibility = 0.9f))),
        )
        assertEquals(BodyOrientation(BodySide.LEFT, 1), SideSelector.select(frames, keyJoints, 1f))
    }

    @Test
    fun noUsableFramesIsNull() {
        assertNull(SideSelector.select(emptyList(), keyJoints, 1f))
        assertNull(SideSelector.select(listOf(TestPoses.frame(0, null)), keyJoints, 1f))
    }
}
