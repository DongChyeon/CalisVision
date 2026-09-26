package com.calisvision.domain.geometry

import com.calisvision.domain.model.Landmark
import kotlin.math.acos
import kotlin.math.sign

object AngleCalculator {

    /** Unsigned angle at vertex [b] in degrees (0..180); coincident points count as straight (180). */
    fun angle(a: Landmark, b: Landmark, c: Landmark, aspect: Float): Float {
        val ba = a.toVec(aspect) - b.toVec(aspect)
        val bc = c.toVec(aspect) - b.toVec(aspect)
        val norm = ba.length() * bc.length()
        if (norm == 0f) return 180f
        val cos = (ba.dot(bc) / norm).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos).toDouble()).toFloat()
    }

    /** sign(cross(c − a, front)); fixed once per video by [SideSelector]. */
    fun frontSign(a: Landmark, c: Landmark, front: Vec2, aspect: Float): Int =
        (c.toVec(aspect) - a.toVec(aspect)).cross(Vec2(front.x * aspect, front.y)).sign.toInt()

    /**
     * dev = sign(cross(c − a, b − a)) · frontSign · (180 − angle).
     * Positive: [b] bulges toward the front side (hyperextension); negative: flexion.
     */
    fun signedDeviation(a: Landmark, b: Landmark, c: Landmark, aspect: Float, frontSign: Int): Float {
        val ac = c.toVec(aspect) - a.toVec(aspect)
        val bulge = ac.cross(b.toVec(aspect) - a.toVec(aspect)).sign.toInt()
        return bulge * frontSign * (180f - angle(a, b, c, aspect))
    }

    fun signedDeviation(a: Landmark, b: Landmark, c: Landmark, aspect: Float, front: Vec2): Float =
        signedDeviation(a, b, c, aspect, frontSign(a, c, front, aspect))

    /** Display angle θ = 180 + dev. */
    fun signedAngle(a: Landmark, b: Landmark, c: Landmark, aspect: Float, frontSign: Int): Float =
        180f + signedDeviation(a, b, c, aspect, frontSign)
}
