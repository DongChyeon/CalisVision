package com.calisvision.ui.result

import com.calisvision.domain.analysis.FaultEvaluator
import com.calisvision.domain.analysis.FaultSegment
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.PoseRule
import com.calisvision.domain.rules.Violation

/** Same bounds as FaultEvaluator, for a single sample: which side of [threshold] the signed angle θ falls on. */
fun violationOf(angle: Float, threshold: AngleThreshold): Violation? = when (threshold) {
    is AngleThreshold.Deviation -> {
        val dev = angle - 180f
        when {
            dev > threshold.maxExtensionDeg -> Violation.EXTENSION
            dev < -threshold.maxFlexionDeg -> Violation.FLEXION
            else -> null
        }
    }
    is AngleThreshold.Range -> when {
        angle < threshold.min -> Violation.BELOW
        angle > threshold.max -> Violation.ABOVE
        else -> null
    }
}

/** A sample breaks [rule] only when its violation maps to a fault (e.g. mild elbow hyperextension does not). */
fun PoseRule.isBrokenBy(angle: Float?, threshold: AngleThreshold): Boolean =
    angle != null && violationOf(angle, threshold)?.let { faults[it] } != null

/**
 * User decision 2026-09-27: only faults inside the hold segment are listed; the rest are drawn dimmed on the timeline.
 * "Inside" means at least [FaultEvaluator.MIN_SAMPLES] of the fault's samples fall in [hold], so a kick-up fault that
 * only grazes the hold boundary stays out of the list.
 */
fun FaultSegment.isInHold(hold: IntRange?): Boolean {
    if (hold == null) return false
    val overlap = minOf(range.last, hold.last) - maxOf(range.first, hold.first) + 1
    return overlap >= FaultEvaluator.MIN_SAMPLES
}
