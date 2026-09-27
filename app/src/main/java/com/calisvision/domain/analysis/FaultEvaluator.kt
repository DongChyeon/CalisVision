package com.calisvision.domain.analysis

import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.Exercise
import com.calisvision.domain.rules.PoseFault
import com.calisvision.domain.rules.PoseRule
import com.calisvision.domain.rules.RuleId
import com.calisvision.domain.rules.Violation
import kotlin.math.abs

/**
 * [range] is in sampleIndex; [angles] holds each contributing rule's angle at its peak.
 * [peakDeviation] is the signed θ − 180 with the largest magnitude; [boundaryExcess] the most degrees beyond the violated bound (≥ 0).
 */
data class FaultSegment(
    val fault: PoseFault,
    val range: IntRange,
    val angles: Map<RuleId, Float>,
    val peakDeviation: Float,
    val boundaryExcess: Float,
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

    /**
     * [segment] restricted to the samples in [window] (sampleIndex): range, angles, peak and excess are re-derived from
     * the samples there that still break a contributing rule into the same fault. Null when none do.
     */
    fun clip(
        segment: FaultSegment,
        window: IntRange,
        timeline: AngleTimeline,
        rules: List<PoseRule>,
        overrides: Map<RuleId, AngleThreshold> = emptyMap(),
    ): FaultSegment? {
        val first = maxOf(segment.range.first, window.first)
        val last = minOf(segment.range.last, window.last)
        if (first > last) return null
        val contributing = rules.filter { it.id in segment.angles }
        val hits = timeline.frames.filter { it.sampleIndex in first..last }.flatMap { frame ->
            contributing.mapNotNull { rule ->
                val angle = frame.angles[rule.id] ?: return@mapNotNull null
                val check = violation(angle, overrides[rule.id] ?: rule.threshold) ?: return@mapNotNull null
                Hit(rule.id, angle, check.second).takeIf { rule.faults[check.first]?.id == segment.faultId }
            }
        }
        if (hits.isEmpty()) return null
        return FaultSegment(segment.fault, first..last, hits.peakAngles(), hits.maxBy { abs(it.deviation) }.deviation, hits.maxOf { it.excess })
    }

    private fun List<Hit>.peakAngles() = groupBy { it.ruleId }.mapValues { (_, h) -> h.maxBy { abs(it.deviation) }.angle }

    private class Hit(val ruleId: RuleId, val angle: Float, val excess: Float) {
        val deviation: Float get() = angle - 180f
    }

    private class Run(val fault: PoseFault, var first: Int, var last: Int, val hits: MutableList<Hit>) {
        fun toSegment(timeline: AngleTimeline): FaultSegment {
            val angles = hits.peakAngles()
            val peak = hits.maxBy { abs(it.deviation) }.deviation
            val excess = hits.maxOf { it.excess }
            return FaultSegment(fault, timeline.frames[first].sampleIndex..timeline.frames[last].sampleIndex, angles, peak, excess)
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
                current.hits += Hit(rule.id, angle, check.second)
                continue
            }
            current?.takeIf { it.hits.size >= MIN_SAMPLES }?.let(runs::add)
            current = fault?.let { Run(it, i, i, mutableListOf(Hit(rule.id, angle, check.second))) }
            currentViolation = check?.first.takeIf { fault != null }
        }
        current?.takeIf { it.hits.size >= MIN_SAMPLES }?.let(runs::add)
        return runs
    }

    /** Returns the violation and the degrees beyond its bound, or null when the signed angle θ is within [threshold]. */
    private fun violation(angle: Float, threshold: AngleThreshold): Pair<Violation, Float>? = when (threshold) {
        is AngleThreshold.Deviation -> {
            val dev = angle - 180f
            when {
                dev > threshold.maxExtensionDeg -> Violation.EXTENSION to dev - threshold.maxExtensionDeg
                dev < -threshold.maxFlexionDeg -> Violation.FLEXION to -threshold.maxFlexionDeg - dev
                else -> null
            }
        }
        is AngleThreshold.Range -> when {
            angle < threshold.min -> Violation.BELOW to threshold.min - angle
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
