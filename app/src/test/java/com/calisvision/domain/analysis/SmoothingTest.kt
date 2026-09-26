package com.calisvision.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class SmoothingTest {

    @Test
    fun removesSingleSpike() {
        val out = Smoothing.movingMedian(listOf(180f, 180f, 250f, 180f, 180f))
        assertEquals(listOf(180f, 180f, 180f, 180f, 180f), out)
    }

    @Test
    fun skipsNullsAndShrinksAtEdges() {
        val out = Smoothing.movingMedian(listOf(1f, null, 3f, 4f, 5f, 6f))
        assertEquals(listOf(2f, 3f, 3.5f, 4.5f, 4.5f, 5f), out)
    }

    @Test
    fun nullWhenFewerThanHalfValid() {
        val out = Smoothing.movingMedian(listOf(null, null, 1f, null, null, null, null))
        assertEquals(listOf(null, null, null, null, null, null, null), out)
        assertEquals(listOf(null), Smoothing.movingMedian(listOf<Float?>(null)))
    }
}
