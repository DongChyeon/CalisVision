package com.calisvision.domain.geometry

import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark
import kotlin.math.abs

object FrontVector {
    const val MIN_VISIBILITY = 0.5f
    const val MIN_FOOT_AXIS_SIN = 0.3f

    /**
     * Primary: nose − midpoint(ears), or nose − the single visible ear. Fallback: toe − heel on [side],
     * abstaining when the foot is nearly parallel to the shoulder→ankle axis. Null when undecidable.
     */
    fun of(pose: FramePose, side: BodySide, aspect: Float): Vec2? {
        val nose = pose[PoseLandmark.NOSE]?.takeIf { visible(it) }
        val ears = listOfNotNull(pose[PoseLandmark.LEFT_EAR], pose[PoseLandmark.RIGHT_EAR]).filter { visible(it) }
        if (nose != null && ears.isNotEmpty()) {
            return Vec2(nose.x - ears.map { it.x }.average().toFloat(), nose.y - ears.map { it.y }.average().toFloat())
        }
        val toe = pose[side, Joint.FOOT_INDEX]
        val heel = pose[side, Joint.HEEL]
        val shoulder = pose[side, Joint.SHOULDER]
        val ankle = pose[side, Joint.ANKLE]
        if (!visible(toe, heel, shoulder, ankle)) return null
        val foot = toe!!.toVec(aspect) - heel!!.toVec(aspect)
        val axis = ankle!!.toVec(aspect) - shoulder!!.toVec(aspect)
        val norm = foot.length() * axis.length()
        if (norm == 0f || abs(foot.cross(axis)) / norm < MIN_FOOT_AXIS_SIN) return null
        return Vec2(toe.x - heel.x, toe.y - heel.y)
    }

    private fun visible(vararg points: Landmark?) = points.all { it != null && it.visibility >= MIN_VISIBILITY }
}
