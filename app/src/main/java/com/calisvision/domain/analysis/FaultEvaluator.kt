package com.calisvision.domain.analysis

import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.Exercise
import com.calisvision.domain.rules.PoseFault
import com.calisvision.domain.rules.PoseRule
import com.calisvision.domain.rules.RuleId
import com.calisvision.domain.rules.Violation
import kotlin.math.abs

/** [range] is in sampleIndex; [angles] holds each contributing rule's angle at its peak; [peakDeviation] is the signed max-|dev| overshoot. */
data class FaultSegment(
    val fault: PoseFault,
    val range: IntRange,
    val angles: Map<RuleId, Float>,
    val peakDeviation: Float,
) {
    val faultId: String get() = fault.id
}

object FaultEvaluator {
    const val MIN_SAMPLES = 3
    const val MAX_MERGE_GAP = 1

    fun evaluate(timeline: AngleTimeline, exercise: Exercise, overrides: Map<RuleId, AngleThreshold> = emptyMap()) =
        evaluate(timeline, exercise.rules, overrides)

    fun evaluate(timeline: AngleTimeline, rules: List<PoseRule>, overrides: Map<RuleId, AngleThreshold> = emptyMap()): List<FaultSegment> {
        val raw = rules.flatMap { rule -> ruleSegments(timeline, rule, overrides[rule.id] ?: rule.threshold) }
        return raw.groupBy { it.fault.id }.values
            .flatMap { merge(it.sortedBy { s -> s.first }) }
            .map { it.toSegment(timeline) }
            .sortedWith(compareBy({ it.range.first }, { it.faultId }))
    }

    private class Hit(val ruleId: RuleId, val index: Int, val angle: Float, val deviation: Float)

    private class Run(val fault: PoseFault, var first: Int, var last: Int, val hits: MutableList<Hit>) {
        fun toSegment(timeline: AngleTimeline): FaultSegment {
            val angles = hits.groupBy { it.ruleId }.mapValues { (_, h) -> h.maxBy { abs(it.deviation) }.angle }
            val peak = hits.maxBy { abs(it.deviation) }.deviation
            return FaultSegment(fault, timeline.frames[first].sampleIndex..timeline.frames[last].sampleIndex, angles, peak)
        }
    }

    private fun ruleSegments(timeline: AngleTimeline, rule: PoseRule, threshold: AngleThreshold): List<Run> {
        val series = timeline.series(rule.id)
        val runs = mutableListOf<Run>()
        var current: Run? = null
        var currentViolation: Violation? = null
        for ((i, angle) in series.withIndex()) {
            val check = angle?.let { violation(it, threshold) }
            val fault = check?.let { rule.faults[it.first] }
            if (fault != null && current != null && currentViolation == check.first) {
                current.last = i
                current.hits += Hit(rule.id, i, angle, check.second)
                continue
            }
            current?.takeIf { it.hits.size >= MIN_SAMPLES }?.let(runs::add)
            current = fault?.let { Run(it, i, i, mutableListOf(Hit(rule.id, i, angle, check.second))) }
            currentViolation = check?.first.takeIf { fault != null }
        }
        current?.takeIf { it.hits.size >= MIN_SAMPLES }?.let(runs::add)
        return runs
    }

    /** Returns the violation and its signed deviation, or null when the angle is within [threshold]. */
    private fun violation(angle: Float, threshold: AngleThreshold): Pair<Violation, Float>? = when (threshold) {
        is AngleThreshold.Deviation -> {
            val dev = angle - 180f
            when {
                dev > threshold.maxExtensionDeg -> Violation.EXTENSION to dev
                dev < -threshold.maxFlexionDeg -> Violation.FLEXION to dev
                else -> null
            }
        }
        is AngleThreshold.Range -> when {
            angle < threshold.min -> Violation.BELOW to angle - threshold.min
            angle > threshold.max -> Violation.ABOVE to angle - threshold.max
            else -> null
        }
    }

    private fun merge(sorted: List<Run>): List<Run> {
        val merged = mutableListOf<Run>()
        for (run in sorted) {
            val last = merged.lastOrNull()
            if (last != null && run.first - last.last - 1 <= MAX_MERGE_GAP) {
                last.last = maxOf(last.last, run.last)
                last.hits += run.hits
            } else {
                merged += Run(run.fault, run.first, run.last, run.hits.toMutableList())
            }
        }
        return merged
    }
}
