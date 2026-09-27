package com.calisvision.ui.result

import com.calisvision.data.AnalysisSession
import com.calisvision.domain.analysis.AngleFrame
import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.analysis.FaultEvaluator
import com.calisvision.domain.geometry.BodyOrientation
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.domain.model.AnalysisResult
import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark

/**
 * 60 samples, hold 10..49, all angles 180° except: 바나나 등 (alignment 195°) at 20..29 inside the hold, a merged
 * 파이크 (alignment 165°, hip 160°) at 40..45 inside the hold, and a 파이크 at 0..4 outside the hold.
 * No frame JPEGs; every frame carries a straight left-side pose so the skeleton draws.
 */
object SyntheticResult {
    const val SAMPLES = 60
    val HOLD = 10..49
    val BANANA = 20..29
    val MERGED_PIKE = 40..45
    val OUTSIDE_PIKE = 0..4

    fun session(sessionId: String = "synthetic", frameDir: String = ""): AnalysisSession {
        val exercise = HandstandKnowledge.exercise
        val timeline = AngleTimeline((0 until SAMPLES).map { i ->
            val alignment = when (i) {
                in BANANA -> 195f
                in MERGED_PIKE, in OUTSIDE_PIKE -> 165f
                else -> 180f
            }
            val hip = if (i in MERGED_PIKE) 160f else 180f
            AngleFrame(
                sampleIndex = i,
                displayTimeMs = i * 100L,
                angles = mapOf(
                    HandstandKnowledge.ALIGNMENT to alignment,
                    HandstandKnowledge.HIP to hip,
                    HandstandKnowledge.SHOULDER_OPEN to 180f,
                    HandstandKnowledge.ELBOW_LOCK to 180f,
                ),
            )
        })
        val pose = straightPose()
        val result = AnalysisResult(
            sessionId = sessionId,
            videoUri = "content://synthetic/video.mp4",
            width = 360,
            height = 640,
            rotationDegrees = 0,
            frames = (0 until SAMPLES).map { FramePose(it, it * 100L, pose) },
            orientation = BodyOrientation(BodySide.LEFT, frontSign = 1),
            holdSegment = HOLD,
            timeline = timeline,
            faults = FaultEvaluator.evaluate(timeline, exercise),
            frameDir = frameDir,
        )
        return AnalysisSession(result, exercise)
    }

    private fun straightPose(): List<Landmark> {
        val side = mapOf(
            Joint.WRIST to 0.9f, Joint.ELBOW to 0.8f, Joint.SHOULDER to 0.7f, Joint.HIP to 0.45f,
            Joint.KNEE to 0.28f, Joint.ANKLE to 0.1f, Joint.HEEL to 0.08f, Joint.FOOT_INDEX to 0.08f,
        ).mapKeys { it.key.leftIndex }.mapValues { Landmark(0.5f, it.value) }
        return List(PoseLandmark.COUNT) { side[it] ?: Landmark(0.5f, 0.5f, visibility = 0f) }
    }
}
