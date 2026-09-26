package com.calisvision.domain.analysis

object Smoothing {

    /** Centered moving median; nulls are skipped, the window shrinks at edges, and output is null when valid < 50% of the window. */
    fun movingMedian(values: List<Float?>, window: Int = 5): List<Float?> {
        val half = window / 2
        return values.indices.map { i ->
            val slice = values.subList((i - half).coerceAtLeast(0), (i + half + 1).coerceAtMost(values.size))
            val valid = slice.filterNotNull().sorted()
            if (valid.size * 2 < slice.size) null else median(valid)
        }
    }

    private fun median(sorted: List<Float>): Float {
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2f
    }
}
