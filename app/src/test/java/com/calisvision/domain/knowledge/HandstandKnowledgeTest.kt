package com.calisvision.domain.knowledge

import com.calisvision.domain.TestPoses
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.Violation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HandstandKnowledgeTest {
    private val rules = HandstandKnowledge.rules

    @Test
    fun hasFourRulesWithExpectedThresholds() {
        assertEquals(4, rules.size)
        assertEquals(4, rules.map { it.id }.toSet().size)
        val byId = rules.associateBy { it.id }
        assertEquals(AngleThreshold.Deviation(10f, 10f), byId.getValue(HandstandKnowledge.ALIGNMENT).threshold)
        assertEquals(AngleThreshold.Deviation(15f, 15f), byId.getValue(HandstandKnowledge.HIP).threshold)
        assertEquals(AngleThreshold.Range(165f, 180f), byId.getValue(HandstandKnowledge.SHOULDER_OPEN).threshold)
        assertEquals(AngleThreshold.Range(170f, 180f), byId.getValue(HandstandKnowledge.ELBOW_LOCK).threshold)
    }

    @Test
    fun everyFaultIsFullyDescribed() {
        rules.forEach { rule ->
            assertTrue(rule.name.isNotBlank())
            assertTrue(rule.faults.isNotEmpty())
            rule.faults.values.forEach {
                assertTrue(it.id.isNotBlank())
                assertTrue(it.name.isNotBlank())
                assertTrue(it.description.isNotBlank())
                assertTrue(it.correctionHint.isNotBlank())
            }
        }
    }

    @Test
    fun faultsMatchViolationDirection() {
        rules.forEach { rule ->
            val allowed = when (rule.threshold) {
                is AngleThreshold.Deviation -> setOf(Violation.EXTENSION, Violation.FLEXION)
                is AngleThreshold.Range -> setOf(Violation.BELOW, Violation.ABOVE)
            }
            assertTrue(allowed.containsAll(rule.faults.keys))
        }
    }

    @Test
    fun bothPikeFaultsShareIdentity() {
        val byId = rules.associateBy { it.id }
        val alignmentPike = byId.getValue(HandstandKnowledge.ALIGNMENT).faults.getValue(Violation.FLEXION)
        val hipPike = byId.getValue(HandstandKnowledge.HIP).faults.getValue(Violation.FLEXION)
        assertSame(alignmentPike, hipPike)
        assertEquals("파이크", hipPike.name)
        assertEquals("바나나 등", byId.getValue(HandstandKnowledge.ALIGNMENT).faults.getValue(Violation.EXTENSION).name)
        assertEquals("골반 전방경사", byId.getValue(HandstandKnowledge.HIP).faults.getValue(Violation.EXTENSION).name)
    }

    @Test
    fun orientationPriorExpectsHandsBelowFeet() {
        val prior = HandstandKnowledge.exercise.orientationPrior
        assertTrue(prior(TestPoses.frame(0, TestPoses.handstand())))
        val flipped = TestPoses.handstand().mapValues { (_, l) -> l.copy(y = 1f - l.y) }
        assertFalse(prior(TestPoses.frame(0, flipped)))
        assertFalse(prior(TestPoses.frame(0, null)))
    }

    @Test
    fun shootingGuideIsSideView() {
        val guide = HandstandKnowledge.exercise.shootingGuide
        assertEquals("측면", guide.view)
        assertTrue(guide.instruction.contains("2–3m"))
        assertTrue(guide.faultsCovered.isNotEmpty())
    }
}
