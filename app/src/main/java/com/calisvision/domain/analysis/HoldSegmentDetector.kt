package com.calisvision.domain.analysis

import com.calisvision.domain.geometry.toVec
import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint
import com.calisvision.domain.model.MIN_JOINT_VISIBILITY

object HoldSegmentDetector {
    const val WINDOW = 15
    const val MAX_RANGE = 0.02f

    /**
     * Longest run of frames covered by a [WINDOW]-sample window in which the wrist and ankle
     * x and y ranges all stay below [MAX_RANGE] of frame height. Per window and joint, the more visible
     * side that is at least [MIN_JOINT_VISIBILITY] in every frame is used; if neither side is, the window is not still.
     * [frames] are consecutive samples; returns the sampleIndex range or null.
     */
    fun detect(frames: List<FramePose>, aspect: Float): IntRange? {
        if (frames.size < WINDOW) return null
        val stable = BooleanArray(frames.size)
        for (start in 0..frames.size - WINDOW) {
            val window = frames.subList(start, start + WINDOW)
            if (listOf(Joint.WRIST, Joint.ANKLE).all { isStill(window, it, aspect) }) {
                stable.fill(true, start, start + WINDOW)
            }
        }
        var best: IntRange? = null
        var runStart = -1
        for (i in 0..frames.size) {
            if (i < frames.size && stable[i]) {
                if (runStart < 0) runStart = i
            } else if (runStart >= 0) {
                if (best == null || i - runStart > best.last - best.first + 1) best = runStart until i
                runStart = -1
            }
        }
        return best?.let { frames[it.first].sampleIndex..frames[it.last].sampleIndex }
    }

    private fun isStill(window: List<FramePose>, joint: Joint, aspect: Float): Boolean {
        val points = BodySide.entries
            .map { side -> window.map { it[side, joint]?.takeIf { l -> l.visibility >= MIN_JOINT_VISIBILITY } } }
            .filter { side -> side.all { it != null } }
            .maxByOrNull { side -> side.sumOf { it!!.visibility.toDouble() } }
            ?.map { it!!.toVec(aspect) }
            ?: return false
        return range(points.map { it.x }) < MAX_RANGE && range(points.map { it.y }) < MAX_RANGE
    }

    private fun range(values: List<Float>) = values.max() - values.min()
}
