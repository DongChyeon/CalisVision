package com.calisvision.domain.rules

sealed interface AngleThreshold {
    /** Applies to the signed angle θ = 180 + dev; below [min] and above [max] are separate violations. */
    data class Range(val min: Float, val max: Float) : AngleThreshold

    /** Applies to signed deviation dev = θ − 180; + is extension, − is flexion. */
    data class Deviation(val maxExtensionDeg: Float, val maxFlexionDeg: Float) : AngleThreshold

    /** Which side of this threshold the signed angle [theta] falls on; null when within it. */
    fun violation(theta: Float): Violation? = when (this) {
        is Deviation -> when {
            theta - 180f > maxExtensionDeg -> Violation.EXTENSION
            theta - 180f < -maxFlexionDeg -> Violation.FLEXION
            else -> null
        }
        is Range -> when {
            theta < min -> Violation.BELOW
            theta > max -> Violation.ABOVE
            else -> null
        }
    }

    /** Degrees [theta] lies beyond the bound it violates; 0 when within this threshold. */
    fun excessDeg(theta: Float): Float = when (this) {
        is Deviation -> maxOf(theta - 180f - maxExtensionDeg, -maxFlexionDeg - (theta - 180f), 0f)
        is Range -> maxOf(min - theta, theta - max, 0f)
    }
}

enum class Violation { EXTENSION, FLEXION, BELOW, ABOVE }
