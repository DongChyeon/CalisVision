package com.calisvision.domain.rules

import com.calisvision.domain.geometry.AngleCalculator
import com.calisvision.domain.model.Landmark
import kotlin.math.abs

/** Full-body alignment = the signed deviation with larger |dev| among shoulder and hip vertices (agreed 2026-09-26). */
object AlignmentStrategy {
    fun deviation(wrist: Landmark, shoulder: Landmark, hip: Landmark, ankle: Landmark, aspect: Float, frontSign: Int): Float {
        val atShoulder = AngleCalculator.signedDeviation(wrist, shoulder, hip, aspect, frontSign)
        val atHip = AngleCalculator.signedDeviation(shoulder, hip, ankle, aspect, frontSign)
        return if (abs(atShoulder) >= abs(atHip)) atShoulder else atHip
    }
}
