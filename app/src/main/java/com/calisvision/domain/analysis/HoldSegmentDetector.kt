package com.calisvision.domain.analysis

import com.calisvision.domain.geometry.Vec2
import com.calisvision.domain.geometry.toVec
import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint
import com.calisvision.domain.model.MIN_JOINT_VISIBILITY
import kotlin.math.abs

/**
 * Two passes: a window-stillness seed locates the hold, then the segment is every contiguous frame whose
 * wrist & ankle stay near the seed's median pose. Seed boundaries sit where window ranges hover around
 * [MAX_RANGE] and flip under landmark noise; distances to a median pose have wide margins at entry/exit.
 */
object HoldSegmentDetector {
    const val WINDOW = 15
    const val MAX_RANGE = 0.02f

    /** Per-frame distance from the median hold pose (Chebyshev, fraction of frame height). */
    const val HOLD_TOLERANCE = 0.03f

    private val JOINTS = listOf(Joint.WRIST, Joint.ANKLE)

    fun detect(frames: List<FramePose>, aspect: Float): IntRange? {
        val seed = stillSeed(frames, aspect) ?: return null
        val seedFrames = frames.subList(seed.first, seed.last + 1)
        val side = visibleSide(seedFrames) ?: return null
        val reference = JOINTS.map { joint ->
            val points = seedFrames.map { it[side, joint]!!.toVec(aspect) }
            Vec2(median(points.map { it.x }), median(points.map { it.y }))
        }
        val near = BooleanArray(frames.size) { i ->
            JOINTS.zip(reference).all { (joint, ref) ->
                val p = frames[i][side, joint]?.takeIf { it.visibility >= MIN_JOINT_VISIBILITY }?.toVec(aspect)
                p != null && abs(p.x - ref.x) < HOLD_TOLERANCE && abs(p.y - ref.y) < HOLD_TOLERANCE
            }
        }
        return longestRun(near)?.let { frames[it.first].sampleIndex..frames[it.last].sampleIndex }
    }

    /** Longest run of frames covered by a [WINDOW] whose wrist & ankle ranges are all < [MAX_RANGE]. */
    private fun stillSeed(frames: List<FramePose>, aspect: Float): IntRange? {
        if (frames.size < WINDOW) return null
        val stable = BooleanArray(frames.size)
        for (start in 0..frames.size - WINDOW) {
            val window = frames.subList(start, start + WINDOW)
            if (JOINTS.all { isStill(window, it, aspect) }) {
                stable.fill(true, start, start + WINDOW)
            }
        }
        return longestRun(stable)
    }

    private fun longestRun(flags: BooleanArray): IntRange? {
        var best: IntRange? = null
        var runStart = -1
        for (i in 0..flags.size) {
            if (i < flags.size && flags[i]) {
                if (runStart < 0) runStart = i
            } else if (runStart >= 0) {
                if (best == null || i - runStart > best.last - best.first + 1) best = runStart until i
                runStart = -1
            }
        }
        return best
    }

    /** Side whose wrist & ankle are visible in every frame, preferring the higher total visibility. */
    private fun visibleSide(frames: List<FramePose>): BodySide? = BodySide.entries
        .filter { side -> frames.all { f -> JOINTS.all { (f[side, it]?.visibility ?: 0f) >= MIN_JOINT_VISIBILITY } } }
        .maxByOrNull { side -> frames.sumOf { f -> JOINTS.sumOf { f[side, it]!!.visibility.toDouble() } } }

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

    private fun median(values: List<Float>): Float {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}
