package com.calisvision.domain.analysis

data class AlignmentGateResult(
    val passed: Boolean,
    val ratio: Float,
    val holdSamples: Int,
    val reason: String,
)

/** AC-4 gate over the smoothed full-body alignment θ series of the hold segment; null samples count as failures. */
object AlignmentGate {
    const val MIN_HOLD_SAMPLES = 50
    const val MIN_RATIO = 0.95f
    val ALIGNED = 175f..185f

    fun evaluate(smoothedTheta: List<Float?>): AlignmentGateResult {
        val n = smoothedTheta.size
        val ratio = if (n == 0) 0f else smoothedTheta.count { it != null && it in ALIGNED }.toFloat() / n
        return when {
            n < MIN_HOLD_SAMPLES -> AlignmentGateResult(false, ratio, n, "hold $n < $MIN_HOLD_SAMPLES samples")
            ratio < MIN_RATIO -> AlignmentGateResult(false, ratio, n, "aligned ratio $ratio < $MIN_RATIO")
            else -> AlignmentGateResult(true, ratio, n, "aligned ratio $ratio >= $MIN_RATIO over $n samples")
        }
    }
}
