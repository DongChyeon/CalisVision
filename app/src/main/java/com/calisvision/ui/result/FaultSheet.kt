package com.calisvision.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.calisvision.R
import com.calisvision.domain.analysis.FaultSegment
import com.calisvision.domain.rules.PoseRule
import com.calisvision.ui.theme.CalisTheme

@Composable
fun faultTimeRange(fault: FaultSegment): String =
    stringResource(R.string.fault_time_range, fault.range.first * SAMPLE_SECONDS, fault.range.last * SAMPLE_SECONDS)

/** Faults inside the hold segment; the only faults that are listed (user decision 2026-09-27). */
@Composable
fun FaultList(faults: List<FaultSegment>, onSelect: (FaultSegment) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s8)) {
        Text(stringResource(R.string.fault_list_title), style = CalisTheme.typography.label1Bold, color = CalisTheme.colors.labelNeutral)
        if (faults.isEmpty()) {
            Text(stringResource(R.string.fault_list_empty), style = CalisTheme.typography.label1Regular, color = CalisTheme.colors.labelAlternative)
        }
        faults.forEach { fault ->
            Surface(
                onClick = { onSelect(fault) },
                shape = RoundedCornerShape(CalisTheme.radius.component),
                color = CalisTheme.colors.backgroundElevatedNormal,
                border = BorderStroke(CalisTheme.spacing.divider, CalisTheme.colors.lineNormalNormal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fault_item"),
            ) {
                Row(
                    Modifier.padding(horizontal = CalisTheme.spacing.s16, vertical = CalisTheme.spacing.s12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s8),
                ) {
                    Box(Modifier.size(CalisTheme.spacing.s8).background(CalisTheme.appColors.fault, RoundedCornerShape(CalisTheme.radius.small)))
                    Text(fault.fault.name, style = CalisTheme.typography.body1Bold, color = CalisTheme.colors.labelNormal, modifier = Modifier.weight(1f))
                    Text(faultTimeRange(fault), style = CalisTheme.typography.label1Regular, color = CalisTheme.colors.labelAlternative)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CalisTheme.colors.labelAssistive)
                }
            }
        }
    }
}

/** Name, description, hint and time range; a fault merged from two rules (e.g. 파이크) lists both rules' angles (AC-8). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaultSheet(fault: FaultSegment, rules: List<PoseRule>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CalisTheme.colors.backgroundElevatedNormal,
    ) {
        Column(
            Modifier
                .testTag("fault_sheet")
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CalisTheme.spacing.s20)
                .padding(bottom = CalisTheme.spacing.s24)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s12),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fault.fault.name, style = CalisTheme.typography.title3Bold, color = CalisTheme.appColors.fault, modifier = Modifier.weight(1f))
                Text(faultTimeRange(fault), style = CalisTheme.typography.label1Medium, color = CalisTheme.colors.labelAlternative)
            }
            Text(fault.fault.description, style = CalisTheme.typography.body1ReadingRegular, color = CalisTheme.colors.labelNeutral)
            Surface(shape = RoundedCornerShape(CalisTheme.radius.component), color = CalisTheme.colors.fillAlternative, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(CalisTheme.spacing.s16), verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s4)) {
                    Text(stringResource(R.string.fault_hint_title), style = CalisTheme.typography.label1Bold, color = CalisTheme.colors.primaryNormal)
                    Text(fault.fault.correctionHint, style = CalisTheme.typography.body1ReadingRegular, color = CalisTheme.colors.labelNormal)
                }
            }
            Text(stringResource(R.string.fault_angles_title), style = CalisTheme.typography.label1Bold, color = CalisTheme.colors.labelNeutral)
            rules.filter { it.id in fault.angles }.forEach { rule ->
                val angle = fault.angles.getValue(rule.id)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.name, style = CalisTheme.typography.body2Medium, color = CalisTheme.colors.labelNormal, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.fault_angle_value, angle, angle - 180f),
                        style = CalisTheme.typography.body2Bold,
                        color = CalisTheme.appColors.fault,
                    )
                }
            }
            Text(stringResource(R.string.fault_angles_note), style = CalisTheme.typography.caption1Regular, color = CalisTheme.colors.labelAssistive)
        }
    }
}
