package com.calisvision.domain.geometry

import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint

data class BodyOrientation(val side: BodySide, val frontSign: Int)

object SideSelector {

    /**
     * Decides side and front sign once per video by per-frame majority vote over [frames]
     * (hold segment, or the middle 50% of the video). Ties resolve to LEFT / +1.
     */
    fun select(frames: List<FramePose>, keyJoints: List<Joint>, aspect: Float): BodyOrientation? {
        require(keyJoints.isNotEmpty())
        val usable = frames.filter { it.landmarks != null }
        if (usable.isEmpty()) return null

        val leftVotes = usable.count { meanVisibility(it, BodySide.LEFT, keyJoints) >= meanVisibility(it, BodySide.RIGHT, keyJoints) }
        val side = if (leftVotes * 2 >= usable.size) BodySide.LEFT else BodySide.RIGHT

        val signs = usable.mapNotNull { pose ->
            val front = FrontVector.of(pose, side, aspect) ?: return@mapNotNull null
            val shoulder = pose[side, Joint.SHOULDER] ?: return@mapNotNull null
            val ankle = pose[side, Joint.ANKLE] ?: return@mapNotNull null
            AngleCalculator.frontSign(shoulder, ankle, front, aspect).takeIf { it != 0 }
        }
        if (signs.isEmpty()) return null
        val frontSign = if (signs.count { it > 0 } * 2 >= signs.size) 1 else -1
        return BodyOrientation(side, frontSign)
    }

    private fun meanVisibility(pose: FramePose, side: BodySide, joints: List<Joint>): Float =
        joints.map { pose[side, it]?.visibility ?: 0f }.average().toFloat()
}
