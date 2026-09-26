package com.calisvision.domain.model

/** [landmarks] is null when no pose was detected, otherwise it holds 33 MediaPipe entries. */
data class FramePose(
    val sampleIndex: Int,
    val displayTimeMs: Long,
    val landmarks: List<Landmark>?,
) {
    operator fun get(index: Int): Landmark? = landmarks?.getOrNull(index)

    operator fun get(side: BodySide, joint: Joint): Landmark? = get(side.index(joint))
}
