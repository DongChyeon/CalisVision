package com.calisvision.ui.result

import com.calisvision.domain.analysis.FaultEvaluator
import com.calisvision.domain.analysis.FaultSegment

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
