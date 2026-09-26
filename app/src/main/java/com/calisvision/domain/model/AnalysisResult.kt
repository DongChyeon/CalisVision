package com.calisvision.domain.model

import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.analysis.FaultEvaluator
import com.calisvision.domain.analysis.FaultSegment
import com.calisvision.domain.analysis.HoldSegmentDetector
import com.calisvision.domain.geometry.BodyOrientation
import com.calisvision.domain.geometry.SideSelector
import com.calisvision.domain.rules.Exercise

data class AnalysisResult(
    val sessionId: String,
    val videoUri: String,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val frames: List<FramePose>,
    val orientation: BodyOrientation?,
    val holdSegment: IntRange?,
    val timeline: AngleTimeline,
    val faults: List<FaultSegment>,
    val frameDir: String,
) {
    val aspect: Float get() = if (height == 0) 1f else width.toFloat() / height

    companion object {
        fun assemble(
            sessionId: String,
            videoUri: String,
            width: Int,
            height: Int,
            rotationDegrees: Int,
            frames: List<FramePose>,
            exercise: Exercise,
            frameDir: String,
        ): AnalysisResult {
            val aspect = if (height == 0) 1f else width.toFloat() / height
            val hold = HoldSegmentDetector.detect(frames, aspect)
            val window = hold?.let { r -> frames.filter { it.sampleIndex in r } }
                ?: frames.subList(frames.size / 4, frames.size - frames.size / 4)
            val orientation = SideSelector.select(window, exercise.keyJoints, aspect)
            val timeline = orientation?.let { AngleTimeline.build(frames, exercise, it, aspect).smoothed() } ?: AngleTimeline(emptyList())
            return AnalysisResult(
                sessionId = sessionId,
                videoUri = videoUri,
                width = width,
                height = height,
                rotationDegrees = rotationDegrees,
                frames = frames,
                orientation = orientation,
                holdSegment = hold,
                timeline = timeline,
                faults = FaultEvaluator.evaluate(timeline, exercise),
                frameDir = frameDir,
            )
        }
    }
}
