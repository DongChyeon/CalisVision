package com.calisvision.domain.analysis

import com.calisvision.domain.geometry.AngleCalculator
import com.calisvision.domain.geometry.BodyOrientation
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.rules.AlignmentStrategy
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.Exercise
import com.calisvision.domain.rules.PoseRule
import com.calisvision.domain.rules.RuleId

/** Deviation rules hold the signed angle θ = 180 + dev, Range rules the unsigned angle; null when landmarks are missing. */
data class AngleFrame(
    val sampleIndex: Int,
    val displayTimeMs: Long,
    val angles: Map<RuleId, Float?>,
)

/** Raw per-frame angles, independent of threshold values, so faults can be re-evaluated without re-analysis. */
data class AngleTimeline(val frames: List<AngleFrame>) {

    fun series(ruleId: RuleId): List<Float?> = frames.map { it.angles[ruleId] }

    fun smoothed(window: Int = 5): AngleTimeline {
        val ruleIds = frames.flatMap { it.angles.keys }.distinct()
        val smoothedSeries = ruleIds.associateWith { Smoothing.movingMedian(series(it), window) }
        return AngleTimeline(frames.mapIndexed { i, frame ->
            frame.copy(angles = ruleIds.associateWith { smoothedSeries.getValue(it)[i] })
        })
    }

    companion object {
        fun build(poses: List<FramePose>, exercise: Exercise, orientation: BodyOrientation, aspect: Float) =
            AngleTimeline(poses.map { pose ->
                AngleFrame(pose.sampleIndex, pose.displayTimeMs, exercise.rules.associate { it.id to measure(pose, it, orientation, aspect) })
            })

        private fun measure(pose: FramePose, rule: PoseRule, orientation: BodyOrientation, aspect: Float): Float? {
            val p = rule.joints.map { pose[orientation.side, it] ?: return null }
            if (p.size == 4) return 180f + AlignmentStrategy.deviation(p[0], p[1], p[2], p[3], aspect, orientation.frontSign)
            return when (rule.threshold) {
                is AngleThreshold.Deviation -> AngleCalculator.signedAngle(p[0], p[1], p[2], aspect, orientation.frontSign)
                is AngleThreshold.Range -> AngleCalculator.angle(p[0], p[1], p[2], aspect)
            }
        }
    }
}
