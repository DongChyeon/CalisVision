package com.calisvision.domain.analysis

import com.calisvision.domain.TestPoses
import com.calisvision.domain.model.PoseLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HoldSegmentDetectorTest {

    private fun frames(offsets: List<Float?>) = offsets.mapIndexed { i, dx ->
        TestPoses.frame(i, dx?.let { TestPoses.handstand(dx = it, dy = if (i % 2 == 0) 0.002f else 0f) })
    }

    @Test
    fun findsLongestStillRun() {
        val moving = List(10) { it * 0.05f }
        val offsets = moving + List(30) { if (it % 2 == 0) 0.6f else 0.605f } + moving.map { 0.8f + it }
        assertEquals(10..39, HoldSegmentDetector.detect(frames(offsets), aspect = 1f))
    }

    @Test
    fun aspectScalesHorizontalRange() {
        val offsets = List(20) { if (it % 2 == 0) 0f else 0.03f }
        assertNull(HoldSegmentDetector.detect(frames(offsets), aspect = 1f))
        assertEquals(0..19, HoldSegmentDetector.detect(frames(offsets), aspect = 0.5f))
    }

    @Test
    fun missingLandmarksBreakTheHold() {
        val offsets: List<Float?> = List(20) { 0f } + listOf(null) + List(16) { 0f }
        assertEquals(0..19, HoldSegmentDetector.detect(frames(offsets), aspect = 1f))
    }

    @Test
    fun jitteryFarSideIsIgnored() {
        val frames = List(20) { i ->
            val pose = TestPoses.handstand()
            val jitter = if (i % 2 == 0) 0.1f else 0f
            TestPoses.frame(i, pose.mapValues { (index, l) -> if (index % 2 == 0 && index > PoseLandmark.RIGHT_EAR) l.copy(x = l.x + jitter) else l })
        }
        assertEquals(0..19, HoldSegmentDetector.detect(frames, aspect = 1f))
    }

    @Test
    fun framesWithoutVisibleWristBreakTheHold() {
        val hidden = TestPoses.handstand().mapValues { (index, l) ->
            if (index == PoseLandmark.LEFT_WRIST || index == PoseLandmark.RIGHT_WRIST) l.copy(visibility = 0.4f) else l
        }
        val frames = List(20) { TestPoses.frame(it, TestPoses.handstand()) } + TestPoses.frame(20, hidden) +
            List(16) { TestPoses.frame(21 + it, TestPoses.handstand()) }
        assertEquals(0..19, HoldSegmentDetector.detect(frames, aspect = 1f))
    }

    @Test
    fun tooShortIsNull() {
        assertNull(HoldSegmentDetector.detect(frames(List(14) { 0f }), aspect = 1f))
    }
}
