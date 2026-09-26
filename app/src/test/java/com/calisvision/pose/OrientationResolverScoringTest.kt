package com.calisvision.pose

import com.calisvision.domain.TestPoses
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.domain.model.Landmark
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationResolverScoringTest {
    private val exercise = HandstandKnowledge.exercise

    private fun frames(points: Map<Int, Landmark>?) = (0 until 10).map { TestPoses.frame(it, points) }

    private fun upsideDown(points: Map<Int, Landmark>) = points.mapValues { (_, l) -> l.copy(y = 1f - l.y) }

    @Test
    fun uprightWinsWhenInvertedDetectsNothing() {
        assertEquals(0, OrientationResolver.choose(frames(TestPoses.handstand()), frames(null), exercise))
    }

    @Test
    fun invertedWinsWhenItDetectsTheHandstand() {
        assertEquals(180, OrientationResolver.choose(frames(upsideDown(TestPoses.handstand())), frames(TestPoses.handstand()), exercise))
    }

    @Test
    fun tieAndNearTieResolveToZero() {
        val same = frames(TestPoses.handstand())
        assertEquals(0, OrientationResolver.choose(same, same, exercise))
        val slightlyBetter = frames(TestPoses.handstand(sideVisibility = 1f))
        val base = frames(TestPoses.handstand(sideVisibility = 0.98f))
        assertEquals(0, OrientationResolver.choose(base, slightlyBetter, exercise))
        assertEquals(180, OrientationResolver.choose(frames(TestPoses.handstand(sideVisibility = 0.9f)), slightlyBetter, exercise))
    }

    @Test
    fun scoreWeighsVisibilityAndPrior() {
        assertEquals(0.7f * 0.65f + 0.3f, OrientationResolver.score(frames(TestPoses.handstand()), exercise), 1e-4f)
        assertEquals(0.7f * 0.65f, OrientationResolver.score(frames(upsideDown(TestPoses.handstand())), exercise), 1e-4f)
        assertEquals(0f, OrientationResolver.score(frames(null), exercise), 0f)
        val half = frames(TestPoses.handstand()).take(5) + frames(null).take(5)
        assertEquals(0.5f * (0.7f * 0.65f + 0.3f), OrientationResolver.score(half, exercise), 1e-4f)
    }

    @Test
    fun samplesTenFramesAcrossTheMiddleHalf() {
        val indices = OrientationResolver.sampleIndices(101)
        assertEquals(10, indices.size)
        assertEquals(25, indices.first())
        assertEquals(75, indices.last())
        assertEquals(listOf(0), OrientationResolver.sampleIndices(1))
        assertEquals(emptyList<Int>(), OrientationResolver.sampleIndices(0))
    }
}
