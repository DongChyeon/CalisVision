package com.calisvision.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calisvision.R
import com.calisvision.domain.knowledge.ExerciseCatalog
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.PoseRule
import com.calisvision.domain.rules.Violation
import com.calisvision.ui.components.CalisTopBar
import com.calisvision.ui.theme.CalisTheme
import kotlin.math.roundToInt

/** Slider bounds in degrees; wide enough for every default with room on both sides. */
private val RANGE_BOUNDS = 130f..230f
private val DEVIATION_BOUNDS = 0f..30f

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val thresholds by viewModel.thresholds.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { CalisTopBar(stringResource(R.string.settings_title), onBack = onBack) },
        containerColor = CalisTheme.colors.backgroundNormalNormal,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CalisTheme.spacing.s20)
                .padding(bottom = CalisTheme.spacing.s24),
            verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s12),
        ) {
            Text(stringResource(R.string.settings_intro), style = CalisTheme.typography.label1Regular, color = CalisTheme.colors.labelAlternative)
            ExerciseCatalog.all.forEach { exercise ->
                exercise.rules.forEach { rule ->
                    RuleCard(rule, thresholds[rule.id] ?: rule.threshold, onChange = { viewModel.set(rule.id, it) })
                }
            }
            OutlinedButton(
                onClick = viewModel::reset,
                shape = RoundedCornerShape(CalisTheme.radius.component),
                border = BorderStroke(CalisTheme.spacing.divider, CalisTheme.colors.lineNormalNormal),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_reset), style = CalisTheme.typography.body1Medium, color = CalisTheme.colors.labelNormal)
            }
        }
    }
}

/** Sliders edit a local draft and save once the drag ends, so DataStore sees one write per gesture. */
@Composable
private fun RuleCard(rule: PoseRule, saved: AngleThreshold, onChange: (AngleThreshold) -> Unit) {
    var draft by remember(saved) { mutableStateOf(saved) }
    Surface(
        shape = RoundedCornerShape(CalisTheme.radius.component),
        color = CalisTheme.colors.backgroundElevatedNormal,
        border = BorderStroke(CalisTheme.spacing.divider, CalisTheme.colors.lineNormalNormal),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(CalisTheme.spacing.s16), verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s4)) {
            Text(rule.name, style = CalisTheme.typography.headline2Bold, color = CalisTheme.colors.labelNormal)
            when (val t = draft) {
                is AngleThreshold.Range -> {
                    ValueRow(stringResource(R.string.settings_range_label), stringResource(R.string.settings_range_value, t.min, t.max))
                    RangeSlider(
                        value = t.min..t.max,
                        onValueChange = { draft = AngleThreshold.Range(it.start.roundToInt().toFloat(), it.endInclusive.roundToInt().toFloat()) },
                        onValueChangeFinished = { onChange(draft) },
                        valueRange = RANGE_BOUNDS,
                        steps = stepsOf(RANGE_BOUNDS),
                        colors = sliderColors(),
                    )
                }
                is AngleThreshold.Deviation -> {
                    val extensionName = rule.faults[Violation.EXTENSION]?.name ?: stringResource(R.string.angle_extension)
                    val flexionName = rule.faults[Violation.FLEXION]?.name ?: stringResource(R.string.angle_flexion)
                    ValueRow(stringResource(R.string.settings_deviation_label, extensionName), stringResource(R.string.settings_deviation_plus, t.maxExtensionDeg))
                    Slider(
                        value = t.maxExtensionDeg,
                        onValueChange = { draft = t.copy(maxExtensionDeg = it.roundToInt().toFloat()) },
                        onValueChangeFinished = { onChange(draft) },
                        valueRange = DEVIATION_BOUNDS,
                        steps = stepsOf(DEVIATION_BOUNDS),
                        colors = sliderColors(),
                    )
                    ValueRow(stringResource(R.string.settings_deviation_label, flexionName), stringResource(R.string.settings_deviation_minus, t.maxFlexionDeg))
                    Slider(
                        value = t.maxFlexionDeg,
                        onValueChange = { draft = t.copy(maxFlexionDeg = it.roundToInt().toFloat()) },
                        onValueChangeFinished = { onChange(draft) },
                        valueRange = DEVIATION_BOUNDS,
                        steps = stepsOf(DEVIATION_BOUNDS),
                        colors = sliderColors(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = CalisTheme.typography.label1Regular, color = CalisTheme.colors.labelAlternative, modifier = Modifier.weight(1f))
        Text(value, style = CalisTheme.typography.label1Bold, color = CalisTheme.colors.labelNormal)
    }
}

/** 1° steps: n degrees span has n − 1 intermediate stops. */
private fun stepsOf(range: ClosedFloatingPointRange<Float>) = (range.endInclusive - range.start).roundToInt() - 1

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = CalisTheme.colors.primaryNormal,
    activeTrackColor = CalisTheme.colors.primaryNormal,
    inactiveTrackColor = CalisTheme.colors.fillNormal,
    activeTickColor = CalisTheme.colors.primaryNormal,
    inactiveTickColor = CalisTheme.colors.fillNormal,
)
