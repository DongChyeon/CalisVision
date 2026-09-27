package com.calisvision.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.calisvision.R
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.PoseRule
import com.calisvision.domain.rules.RuleId
import com.calisvision.domain.rules.Violation
import com.calisvision.ui.theme.CalisTheme
import kotlin.math.abs

/** Below this |θ − 180| a deviation rule reads as straight, with no direction label. */
private const val STRAIGHT_EPSILON_DEG = 0.5f

/** Two-column grid of the exercise's rules at the current sample. */
@Composable
fun AnglePanel(
    rules: List<PoseRule>,
    angles: Map<RuleId, Float?>,
    thresholds: Map<RuleId, AngleThreshold>,
    brokenRules: Set<RuleId>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s8)) {
        rules.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s8)) {
                row.forEach { rule ->
                    AngleCell(rule, angles[rule.id], thresholds[rule.id] ?: rule.threshold, rule.id in brokenRules, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AngleCell(rule: PoseRule, angle: Float?, threshold: AngleThreshold, broken: Boolean, modifier: Modifier) {
    val colors = CalisTheme.colors
    val app = CalisTheme.appColors
    val violation = angle?.let(threshold::violation)
    val accent: Color = when {
        broken -> app.fault
        violation != null -> app.warning
        else -> colors.labelNormal
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CalisTheme.radius.component),
        color = if (broken) app.faultBackground else colors.backgroundElevatedNormal,
        border = BorderStroke(CalisTheme.spacing.divider, if (broken) app.fault else colors.lineNormalNormal),
    ) {
        Column(Modifier.padding(CalisTheme.spacing.s12), verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s2)) {
            Text(rule.name, style = CalisTheme.typography.caption1Medium, color = colors.labelAlternative)
            Text(
                angle?.let { stringResource(R.string.angle_degrees, it) } ?: stringResource(R.string.angle_unmeasurable),
                style = if (angle != null) CalisTheme.typography.heading2Bold else CalisTheme.typography.body1Medium,
                color = if (angle != null) accent else colors.labelAssistive,
            )
            if (angle != null) {
                Text(directionLabel(rule, angle, threshold, violation), style = CalisTheme.typography.caption1Regular, color = if (violation != null) accent else colors.labelAlternative)
            }
        }
    }
}

/**
 * Deviation rules name the side the body bends toward (e.g. 바나나 등 / 파이크 for alignment) with the signed deviation.
 * Range rules say whether θ is inside the range, and otherwise name the violated side.
 */
@Composable
private fun directionLabel(rule: PoseRule, angle: Float, threshold: AngleThreshold, violation: Violation?): String = when (threshold) {
    is AngleThreshold.Deviation -> {
        val dev = angle - 180f
        if (abs(dev) < STRAIGHT_EPSILON_DEG) {
            stringResource(R.string.angle_straight)
        } else {
            val side = if (dev > 0) Violation.EXTENSION else Violation.FLEXION
            val name = rule.faults[side]?.name
                ?: stringResource(if (dev > 0) R.string.angle_extension else R.string.angle_flexion)
            stringResource(R.string.angle_deviation_toward, dev, name)
        }
    }
    is AngleThreshold.Range -> when (violation) {
        Violation.BELOW -> rule.faults[Violation.BELOW]?.name ?: stringResource(R.string.angle_below)
        Violation.ABOVE -> rule.faults[Violation.ABOVE]?.name ?: stringResource(R.string.angle_hyperextension)
        else -> stringResource(R.string.angle_in_range, threshold.min, threshold.max)
    }
}
