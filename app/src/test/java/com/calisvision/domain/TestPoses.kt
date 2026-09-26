package com.calisvision.domain

import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark

/** Hand-built poses in normalized image coords (y down). Unset landmarks are invisible. */
object TestPoses {
    private val hidden = Landmark(0.5f, 0.5f, visibility = 0f)

    fun landmarks(points: Map<Int, Landmark>): List<Landmark> =
        List(PoseLandmark.COUNT) { points[it] ?: hidden }

    fun frame(sampleIndex: Int, points: Map<Int, Landmark>?) =
        FramePose(sampleIndex, sampleIndex * 100L, points?.let(::landmarks))

    /**
     * Straight handstand seen from the LEFT side, face (front) toward +x, hands at the bottom.
     * [hipDx] shifts the hip, [shoulderDx] the shoulder along x (positive = toward the face).
     */
    fun handstand(
        hipDx: Float = 0f,
        shoulderDx: Float = 0f,
        faceVisibility: Float = 1f,
        sideVisibility: Float = 1f,
        otherSideVisibility: Float = 0.3f,
        dx: Float = 0f,
        dy: Float = 0f,
    ): Map<Int, Landmark> {
        fun p(x: Float, y: Float, v: Float) = Landmark(x + dx, y + dy, visibility = v)
        val side = mapOf(
            PoseLandmark.LEFT_WRIST to p(0.5f, 0.9f, sideVisibility),
            PoseLandmark.LEFT_ELBOW to p(0.5f, 0.8f, sideVisibility),
            PoseLandmark.LEFT_SHOULDER to p(0.5f + shoulderDx, 0.7f, sideVisibility),
            PoseLandmark.LEFT_HIP to p(0.5f + hipDx, 0.45f, sideVisibility),
            PoseLandmark.LEFT_KNEE to p(0.5f, 0.28f, sideVisibility),
            PoseLandmark.LEFT_ANKLE to p(0.5f, 0.1f, sideVisibility),
            PoseLandmark.LEFT_HEEL to p(0.5f, 0.08f, sideVisibility),
            PoseLandmark.LEFT_FOOT_INDEX to p(0.54f, 0.08f, sideVisibility),
        )
        val other = side.map { (i, l) -> (i + 1) to l.copy(visibility = otherSideVisibility) }.toMap()
        val face = mapOf(
            PoseLandmark.NOSE to p(0.53f, 0.78f, faceVisibility),
            PoseLandmark.LEFT_EAR to p(0.5f, 0.78f, faceVisibility),
            PoseLandmark.RIGHT_EAR to p(0.5f, 0.78f, faceVisibility),
        )
        return side + other + face
    }
}
