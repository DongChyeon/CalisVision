package com.calisvision.domain.geometry

import com.calisvision.domain.TestPoses
import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrontVectorTest {

    @Test
    fun primaryUsesNoseMinusEarMidpoint() {
        val front = FrontVector.of(TestPoses.frame(0, TestPoses.handstand()), BodySide.LEFT, 1f)!!
        assertEquals(0.03f, front.x, 1e-4f)
        assertEquals(0f, front.y, 1e-4f)
    }

    @Test
    fun fallsBackToToeMinusHeelWhenFaceHidden() {
        val front = FrontVector.of(TestPoses.frame(0, TestPoses.handstand(faceVisibility = 0.4f)), BodySide.LEFT, 1f)!!
        assertEquals(0.04f, front.x, 1e-4f)
        assertEquals(0f, front.y, 1e-4f)
    }

    @Test
    fun singleVisibleEarIsUsedAlone() {
        val pose = TestPoses.handstand() +
            (PoseLandmark.LEFT_EAR to Landmark(0.49f, 0.76f, visibility = 0.9f)) +
            (PoseLandmark.RIGHT_EAR to Landmark(0.2f, 0.2f, visibility = 0.1f))
        val front = FrontVector.of(TestPoses.frame(0, pose), BodySide.LEFT, 1f)!!
        assertEquals(0.04f, front.x, 1e-4f)
        assertEquals(0.02f, front.y, 1e-4f)
    }

    @Test
    fun footParallelToBodyAxisAbstains() {
        val pose = TestPoses.handstand(faceVisibility = 0.4f) +
            (PoseLandmark.LEFT_FOOT_INDEX to Landmark(0.51f, 0.04f))
        assertNull(FrontVector.of(TestPoses.frame(0, pose), BodySide.LEFT, 1f))
    }

    @Test
    fun nullWhenFaceAndFeetHidden() {
        val pose = TestPoses.handstand(faceVisibility = 0.4f, sideVisibility = 0.4f)
        assertNull(FrontVector.of(TestPoses.frame(0, pose), BodySide.LEFT, 1f))
        assertNull(FrontVector.of(TestPoses.frame(0, null), BodySide.LEFT, 1f))
    }
}
