package com.calisvision.domain.rules

import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint

@JvmInline
value class RuleId(val value: String)

data class PoseFault(
    val id: String,
    val name: String,
    val description: String,
    val correctionHint: String,
)

/** [joints] are three joints (a, vertex, c); [faults] maps only the violations that apply. */
data class PoseRule(
    val id: RuleId,
    val name: String,
    val joints: List<Joint>,
    val threshold: AngleThreshold,
    val faults: Map<Violation, PoseFault>,
) {
    init {
        require(joints.size == 3)
    }
}

data class ShootingGuide(
    val view: String,
    val faultsCovered: List<String>,
    val instruction: String,
)

data class Exercise(
    val id: String,
    val name: String,
    val rules: List<PoseRule>,
    val keyJoints: List<Joint>,
    val orientationPrior: (FramePose) -> Boolean,
    val shootingGuide: ShootingGuide,
)
