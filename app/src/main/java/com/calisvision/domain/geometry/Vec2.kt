package com.calisvision.domain.geometry

import com.calisvision.domain.model.Landmark
import kotlin.math.sqrt

data class Vec2(val x: Float, val y: Float) {
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)

    fun cross(o: Vec2): Float = x * o.y - y * o.x

    fun dot(o: Vec2): Float = x * o.x + y * o.y

    fun length(): Float = sqrt(x * x + y * y)
}

/** Scales x by [aspect] (= width / height) so both axes are in frame-height units. */
fun Landmark.toVec(aspect: Float) = Vec2(x * aspect, y)
