package com.calisvision.domain.knowledge

import com.calisvision.domain.TestPoses
import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.analysis.FaultEvaluator
import com.calisvision.domain.geometry.BodyOrientation
import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.Joint
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.Exercise
import com.calisvision.domain.rules.PoseFault
import com.calisvision.domain.rules.PoseRule
import com.calisvision.domain.rules.RuleId
import com.calisvision.domain.rules.ShootingGuide
import com.calisvision.domain.rules.Violation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {

    @Test
    fun looksUpHandstandById() {
        assertSame(HandstandKnowledge.exercise, ExerciseCatalog.byId("handstand"))
        assertNull(ExerciseCatalog.byId("unknown"))
    }

    /** AC-12: a new exercise is evaluated by the generic analysis code without changes. */
    @Test
    fun dummyExerciseRunsThroughGenericAnalysis() {
        val sag = PoseFault("dummy_sag", "허리 처짐", "허리가 아래로 처집니다.", "복부에 힘을 주세요.")
        val ruleId = RuleId("dummy.hip")
        val dummy = Exercise(
            id = "dummy",
            name = "더미",
            rules = listOf(
                PoseRule(ruleId, "몸통 일직선", listOf(Joint.SHOULDER, Joint.HIP, Joint.KNEE), AngleThreshold.Deviation(5f, 5f), mapOf(Violation.EXTENSION to sag)),
            ),
            keyJoints = listOf(Joint.SHOULDER, Joint.HIP, Joint.KNEE),
            orientationPrior = { pose -> (pose[BodySide.LEFT, Joint.SHOULDER]?.y ?: 0f) < (pose[BodySide.LEFT, Joint.HIP]?.y ?: 0f) },
            shootingGuide = ShootingGuide("측면", listOf(sag.name), "옆에서 촬영하세요."),
        )
        val poses = (0 until 4).map { TestPoses.frame(it, TestPoses.handstand(hipDx = 0.06f)) }
        assertFalse(dummy.orientationPrior(poses[0]))

        val timeline = AngleTimeline.build(poses, dummy, BodyOrientation(BodySide.LEFT, 1), aspect = 1f)
        val segment = FaultEvaluator.evaluate(timeline, dummy).single()
        assertEquals(sag, segment.fault)
        assertEquals(0..3, segment.range)
        assertTrue(segment.peakDeviation > 5f)
    }
}
