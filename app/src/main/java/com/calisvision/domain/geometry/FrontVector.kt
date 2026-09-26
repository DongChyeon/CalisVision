package com.calisvision.domain.geometry

import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.PoseLandmark

object FrontVector {
    const val MIN_VISIBILITY = 0.5f

    /** Primary: nose − midpoint(ears). Fallback: toe − heel on [side]. Null when neither is visible enough. */
    fun of(pose: FramePose, side: BodySide): Vec2? {
        val nose = pose[PoseLandmark.NOSE]
        val leftEar = pose[PoseLandmark.LEFT_EAR]
        val rightEar = pose[PoseLandmark.RIGHT_EAR]
        if (visible(nose, leftEar, rightEar)) {
            return Vec2(nose!!.x - (leftEar!!.x + rightEar!!.x) / 2f, nose.y - (leftEar.y + rightEar.y) / 2f)
        }
        val toe = pose[side, Joint.FOOT_INDEX]
        val heel = pose[side, Joint.HEEL]
        if (visible(toe, heel)) {
            return Vec2(toe!!.x - heel!!.x, toe.y - heel.y)
        }
        return null
    }

    private fun visible(vararg points: Landmark?) = points.all { it != null && it.visibility >= MIN_VISIBILITY }
}
