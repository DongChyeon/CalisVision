package com.calisvision.domain.rules

sealed interface AngleThreshold {
    /** Applies to unsigned angles (0..180). */
    data class Range(val min: Float, val max: Float) : AngleThreshold

    /** Applies to signed deviation dev = θ − 180; + is extension, − is flexion. */
    data class Deviation(val maxExtensionDeg: Float, val maxFlexionDeg: Float) : AngleThreshold
}

enum class Violation { EXTENSION, FLEXION, BELOW, ABOVE }
