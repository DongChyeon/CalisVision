package com.calisvision.domain.analysis

import com.calisvision.domain.rules.PoseRule

data class ReferenceHoldGateResult(
    val passed: Boolean,
    val reasons: List<String>,
    val holdSamples: Int,
    val nullSamples: Int,
    val meanTheta: Float?,
    val minTheta: Float?,
    val maxTheta: Float?,
    val holdFaults: List<FaultSegment>,
)

/**
 * AC-4 (ADR-0008) on a straight reference hold: [rule] at its frozen threshold yields no in-hold fault
 * (≥ [FaultEvaluator.MIN_SAMPLES] overlap with the hold, clipped to it — as listed on the result screen), and the
 * mean of the smoothed θ over the hold lies in [MEAN_RANGE]. [timeline] is the full smoothed timeline; [hold] is in sampleIndex.
 */
object ReferenceHoldGate {
    const val MIN_HOLD_SAMPLES = 50
    const val MAX_NULL_RATIO = 0.05f
    val MEAN_RANGE = 175f..185f

    fun evaluate(timeline: AngleTimeline, hold: IntRange, rule: PoseRule): ReferenceHoldGateResult {
        val theta = timeline.frames.filter { it.sampleIndex in hold }.map { it.angles[rule.id] }
        val n = theta.size
        val valid = theta.filterNotNull()
        val nulls = n - valid.size
        val mean = valid.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val faults = FaultEvaluator.evaluate(timeline, listOf(rule))
            .filter { minOf(it.range.last, hold.last) - maxOf(it.range.first, hold.first) + 1 >= FaultEvaluator.MIN_SAMPLES }
            .mapNotNull { FaultEvaluator.clip(it, hold, timeline, listOf(rule)) }

        val reasons = buildList {
            if (n < MIN_HOLD_SAMPLES) add("hold $n < $MIN_HOLD_SAMPLES samples")
            if (nulls > n * MAX_NULL_RATIO) add("null $nulls/$n > ${MAX_NULL_RATIO * 100}%")
            if (mean == null || mean !in MEAN_RANGE) add("mean θ $mean outside $MEAN_RANGE")
            faults.forEach { add("in-hold fault ${it.faultId} ${it.range} (peak ${it.peakDeviation})") }
        }
        return ReferenceHoldGateResult(reasons.isEmpty(), reasons, n, nulls, mean, valid.minOrNull(), valid.maxOrNull(), faults)
    }
}
