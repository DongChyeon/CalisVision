package com.calisvision.domain.model

/** Normalized image coordinates in [0, 1]; y grows downward. */
data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1f,
)

/** Joints below this visibility are treated as missing. */
const val MIN_JOINT_VISIBILITY = 0.5f
