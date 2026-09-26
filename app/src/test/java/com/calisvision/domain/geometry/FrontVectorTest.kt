package com.calisvision.domain.geometry

import com.calisvision.domain.TestPoses
import com.calisvision.domain.model.BodySide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrontVectorTest {

    @Test
    fun primaryUsesNoseMinusEarMidpoint() {
        val front = FrontVector.of(TestPoses.frame(0, TestPoses.handstand()), BodySide.LEFT)!!
        assertEquals(0.03f, front.x, 1e-4f)
        assertEquals(0f, front.y, 1e-4f)
    }

    @Test
    fun fallsBackToToeMinusHeelWhenFaceHidden() {
        val front = FrontVector.of(TestPoses.frame(0, TestPoses.handstand(faceVisibility = 0.4f)), BodySide.LEFT)!!
        assertEquals(0.04f, front.x, 1e-4f)
        assertEquals(0f, front.y, 1e-4f)
    }

    @Test
    fun nullWhenFaceAndFeetHidden() {
        val pose = TestPoses.handstand(faceVisibility = 0.4f, sideVisibility = 0.4f)
        assertNull(FrontVector.of(TestPoses.frame(0, pose), BodySide.LEFT))
        assertNull(FrontVector.of(TestPoses.frame(0, null), BodySide.LEFT))
    }
}
