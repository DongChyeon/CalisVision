package com.calisvision.domain.model

import com.calisvision.domain.TestPoses
import com.calisvision.domain.geometry.BodyOrientation
import com.calisvision.domain.knowledge.HandstandKnowledge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisResultAssembleTest {
    private fun assemble(frames: List<FramePose>) =
        AnalysisResult.assemble("s", "content://v", 1000, 1000, 180, frames, HandstandKnowledge.exercise, "/tmp/s")

    @Test
    fun straightHoldHasNoFaults() {
        val result = assemble((0 until 30).map { TestPoses.frame(it, TestPoses.handstand()) })
        assertEquals(0..29, result.holdSegment)
        assertEquals(BodyOrientation(BodySide.LEFT, 1), result.orientation)
        assertEquals(30, result.timeline.frames.size)
        assertEquals(emptyList<Any>(), result.faults)
        assertEquals(180, result.rotationDegrees)
    }

    @Test
    fun archedHoldReportsBanana() {
        val result = assemble((0 until 30).map { TestPoses.frame(it, TestPoses.handstand(hipDx = 0.06f)) })
        val banana = result.faults.single { it.fault == HandstandKnowledge.BANANA }
        assertEquals(0..29, banana.range)
    }

    @Test
    fun noDetectionsYieldEmptyAnalysis() {
        val result = assemble((0 until 30).map { TestPoses.frame(it, null) })
        assertNull(result.holdSegment)
        assertNull(result.orientation)
        assertTrue(result.timeline.frames.isEmpty())
        assertTrue(result.faults.isEmpty())
    }
}
