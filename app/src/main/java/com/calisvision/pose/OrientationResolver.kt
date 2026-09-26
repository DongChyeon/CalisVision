package com.calisvision.pose

import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.rules.Exercise
import com.calisvision.video.FrameSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.abs
import kotlin.math.roundToInt

object OrientationResolver {
    const val SAMPLE_COUNT = 10
    const val VISIBILITY_WEIGHT = 0.7f
    const val PRIOR_WEIGHT = 0.3f
    const val TIE_EPSILON = 0.01f
    val ROTATIONS = listOf(0, 180)

    suspend fun resolve(source: FrameSource, exercise: Exercise, detector: PoseDetector): Int {
        val poses = ROTATIONS.associateWith { mutableListOf<FramePose>() }
        for (index in sampleIndices(source.info.frameCount)) {
            val frame = source.frameAt(index) ?: continue
            try {
                for (rotation in ROTATIONS) {
                    currentCoroutineContext().ensureActive()
                    poses.getValue(rotation) += FramePose(index, frame.displayTimeMs, detector.detectImage(frame.bitmap, rotation))
                }
            } finally {
                frame.bitmap.recycle()
            }
        }
        return choose(poses.getValue(0), poses.getValue(180), exercise)
    }

    fun sampleIndices(frameCount: Int, count: Int = SAMPLE_COUNT): List<Int> {
        if (frameCount <= 0) return emptyList()
        val last = frameCount - 1
        return (0 until count).map { i ->
            val t = 0.25f + 0.5f * i / (count - 1).coerceAtLeast(1)
            (last * t).roundToInt()
        }.distinct()
    }

    fun choose(upright: List<FramePose>, inverted: List<FramePose>, exercise: Exercise): Int {
        val diff = score(inverted, exercise) - score(upright, exercise)
        return if (abs(diff) < TIE_EPSILON || diff < 0) 0 else 180
    }

    fun score(poses: List<FramePose>, exercise: Exercise): Float {
        if (poses.isEmpty()) return 0f
        val visibility = poses.map { pose ->
            exercise.keyJoints.flatMap { joint -> BodySide.entries.map { pose[it, joint]?.visibility ?: 0f } }.average()
        }.average().toFloat()
        val prior = poses.count { it.landmarks != null && exercise.orientationPrior(it) }.toFloat() / poses.size
        return VISIBILITY_WEIGHT * visibility + PRIOR_WEIGHT * prior
    }
}
