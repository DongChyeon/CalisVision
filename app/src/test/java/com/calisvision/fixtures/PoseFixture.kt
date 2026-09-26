package com.calisvision.fixtures

import com.calisvision.domain.model.FramePose
import com.google.gson.Gson

/**
 * Pose fixture exported by the instrumented pipeline (P1.5/P4), loaded from test resources.
 *
 * ```json
 * {
 *   "width": 1080, "height": 1920,
 *   "frames": [
 *     { "sampleIndex": 0, "displayTimeMs": 0,
 *       "landmarks": [ { "x": 0.5, "y": 0.9, "z": -0.1, "visibility": 0.98 }, ... 33 entries ] },
 *     { "sampleIndex": 1, "displayTimeMs": 100, "landmarks": null }
 *   ]
 * }
 * ```
 * x, y are normalized to the original (upright-corrected) frame, y grows downward; `landmarks` is null when not detected.
 */
data class PoseFixture(
    val width: Int,
    val height: Int,
    val frames: List<FramePose>,
) {
    val aspect: Float get() = width.toFloat() / height

    companion object {
        /** Returns null when the resource is absent. */
        fun load(resource: String): PoseFixture? = PoseFixture::class.java.getResource(resource)?.readText()?.let(::parse)

        fun parse(json: String): PoseFixture = Gson().fromJson(json, PoseFixture::class.java)
    }
}
