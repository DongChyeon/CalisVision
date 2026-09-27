package com.calisvision.domain.rules

import com.calisvision.domain.knowledge.HandstandKnowledge as H
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AngleThresholdTest {

    private val deviation = AngleThreshold.Deviation(maxExtensionDeg = 10f, maxFlexionDeg = 5f)
    private val range = AngleThreshold.Range(min = 170f, max = 190f)

    @Test
    fun deviationBoundsAreInclusive() {
        assertNull(deviation.violation(190f))
        assertNull(deviation.violation(175f))
        assertEquals(Violation.EXTENSION, deviation.violation(190.5f))
        assertEquals(Violation.FLEXION, deviation.violation(174f))
        assertEquals(3f, deviation.excessDeg(193f), 0.001f)
        assertEquals(6f, deviation.excessDeg(169f), 0.001f)
        assertEquals(0f, deviation.excessDeg(180f), 0.001f)
    }

    @Test
    fun rangeBoundsAreInclusive() {
        assertNull(range.violation(170f))
        assertNull(range.violation(190f))
        assertEquals(Violation.BELOW, range.violation(160f))
        assertEquals(Violation.ABOVE, range.violation(195f))
        assertEquals(10f, range.excessDeg(160f), 0.001f)
        assertEquals(5f, range.excessDeg(195f), 0.001f)
        assertEquals(0f, range.excessDeg(180f), 0.001f)
    }

    @Test
    fun faultAtMapsOnlyViolationsWithAFault() {
        val elbow = H.rules.single { it.id == H.ELBOW_LOCK }
        assertEquals(H.BENT_ELBOW, elbow.faultAt(150f))
        assertNull("hyperextension above max is not a fault", elbow.faultAt(200f))
        assertNull(elbow.faultAt(180f))
        val alignment = H.rules.single { it.id == H.ALIGNMENT }
        assertEquals(H.BANANA, alignment.faultAt(195f))
        assertNull("override widens the bound", alignment.faultAt(195f, AngleThreshold.Deviation(20f, 10f)))
    }
}
