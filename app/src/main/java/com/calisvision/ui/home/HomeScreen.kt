package com.calisvision.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.calisvision.R
import com.calisvision.domain.knowledge.ExerciseCatalog
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.domain.rules.Exercise
import com.calisvision.ui.theme.CalisTheme

/** Exercises the result screen can analyze; any other catalog entry is shown as coming soon. */
private val SUPPORTED_EXERCISES = setOf(HandstandKnowledge.exercise.id)

@Composable
fun HomeScreen(
    onExerciseSelected: (exerciseId: String) -> Unit,
) {
    val supported = ExerciseCatalog.all.filter { it.id in SUPPORTED_EXERCISES }
    Surface(color = CalisTheme.colors.backgroundNormalNormal, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(CalisTheme.spacing.s20),
            verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s12),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s12),
            ) {
                Text(stringResource(R.string.app_name), style = CalisTheme.typography.title2Bold, color = CalisTheme.colors.labelStrong)
                Text(stringResource(R.string.home_subtitle), style = CalisTheme.typography.body2Regular, color = CalisTheme.colors.labelAlternative)
                Spacer(Modifier.padding(top = CalisTheme.spacing.s8))
                Text(stringResource(R.string.home_section_exercises), style = CalisTheme.typography.label1Bold, color = CalisTheme.colors.labelNeutral)
                ExerciseCatalog.all.forEach { exercise ->
                    val ready = exercise.id in SUPPORTED_EXERCISES
                    ExerciseCard(
                        name = exercise.name,
                        description = if (ready) exerciseDescription(exercise) else null,
                        ready = ready,
                        onClick = { onExerciseSelected(exercise.id) },
                    )
                }
                stringArrayResource(R.array.home_coming_soon_exercises).forEach { name ->
                    ExerciseCard(name = name, description = null, ready = false, onClick = {})
                }
            }
            Button(
                onClick = { supported.firstOrNull()?.let { onExerciseSelected(it.id) } },
                enabled = supported.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CalisTheme.radius.component),
            ) {
                Text(stringResource(R.string.home_start), style = CalisTheme.typography.body1Bold)
            }
        }
    }
}

@Composable
private fun exerciseDescription(exercise: Exercise): String? = when (exercise.id) {
    HandstandKnowledge.exercise.id -> stringResource(R.string.home_exercise_desc_handstand)
    else -> null
}

@Composable
private fun ExerciseCard(name: String, description: String?, ready: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = ready,
        shape = RoundedCornerShape(CalisTheme.radius.component),
        color = CalisTheme.colors.backgroundElevatedNormal,
        border = BorderStroke(CalisTheme.spacing.divider, CalisTheme.colors.lineNormalNormal),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(CalisTheme.spacing.s16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s12),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s4)) {
                Text(
                    name,
                    style = CalisTheme.typography.headline2Bold,
                    color = if (ready) CalisTheme.colors.labelNormal else CalisTheme.colors.labelAssistive,
                )
                description?.let {
                    Text(it, style = CalisTheme.typography.label1Regular, color = CalisTheme.colors.labelAlternative)
                }
            }
            if (!ready) {
                Surface(
                    shape = RoundedCornerShape(CalisTheme.radius.large),
                    color = CalisTheme.colors.fillAlternative,
                ) {
                    Text(
                        stringResource(R.string.home_coming_soon),
                        style = CalisTheme.typography.caption1Medium,
                        color = CalisTheme.colors.labelAlternative,
                        modifier = Modifier.padding(horizontal = CalisTheme.spacing.s8, vertical = CalisTheme.spacing.s2),
                    )
                }
            }
        }
    }
}
