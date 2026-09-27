package com.calisvision.ui.analysis

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calisvision.R
import com.calisvision.ui.theme.CalisTheme

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    onCompleted: (sessionId: String) -> Unit,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fileName by viewModel.fileName.collectAsStateWithLifecycle()
    val cancelAndExit = {
        viewModel.cancel()
        onExit()
    }
    BackHandler(onBack = cancelAndExit)
    LaunchedEffect(state) {
        (state as? AnalysisUiState.Completed)?.let { onCompleted(it.sessionId) }
    }
    if (state !is AnalysisUiState.Failed) KeepScreenOn()

    Surface(color = CalisTheme.colors.backgroundNormalNormal, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(CalisTheme.spacing.s20),
            verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s16),
        ) {
            when (val s = state) {
                is AnalysisUiState.Failed -> FailedContent(s, onExit)
                else -> ProgressContent(s, fileName ?: stringResource(R.string.analysis_file_fallback), onCancel = cancelAndExit)
            }
        }
    }
}

@Composable
private fun ProgressContent(state: AnalysisUiState, fileName: String, onCancel: () -> Unit) {
    val processing = state as? AnalysisUiState.Processing
    val resolved = state !is AnalysisUiState.ResolvingOrientation
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s16)) {
        Text(stringResource(R.string.analysis_title), style = CalisTheme.typography.title3Bold, color = CalisTheme.colors.labelStrong)
        Text(
            fileName,
            style = CalisTheme.typography.label1Medium,
            color = CalisTheme.colors.labelAlternative,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.testTag("analysis_file_name"),
        )
        StepCard(
            label = stringResource(R.string.analysis_step_orientation),
            status = stringResource(if (resolved) R.string.analysis_step_done else R.string.analysis_step_running),
            progress = if (resolved) 1f else null,
        )
        StepCard(
            label = stringResource(R.string.analysis_step_frames),
            status = processing?.let { stringResource(R.string.analysis_frames_count, it.done, it.total) }
                ?: stringResource(if (resolved) R.string.analysis_step_done else R.string.analysis_step_waiting),
            detail = processing?.remainingSeconds?.let { stringResource(R.string.analysis_remaining, it) },
            progress = when {
                processing != null && processing.total > 0 -> processing.done.toFloat() / processing.total
                state is AnalysisUiState.Completed -> 1f
                else -> 0f
            },
        )
        Text(stringResource(R.string.analysis_keep_screen_on), style = CalisTheme.typography.label1Regular, color = CalisTheme.colors.labelAlternative)
    }
    OutlinedButton(
        onClick = onCancel,
        shape = RoundedCornerShape(CalisTheme.radius.component),
        border = BorderStroke(CalisTheme.spacing.divider, CalisTheme.colors.lineNormalNormal),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.analysis_cancel), style = CalisTheme.typography.body1Medium, color = CalisTheme.colors.labelNormal)
    }
}

/** [progress] null shows an indeterminate bar. */
@Composable
private fun StepCard(label: String, status: String, progress: Float?, detail: String? = null) {
    Surface(
        shape = RoundedCornerShape(CalisTheme.radius.component),
        color = CalisTheme.colors.backgroundElevatedNormal,
        border = BorderStroke(CalisTheme.spacing.divider, CalisTheme.colors.lineNormalNormal),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(CalisTheme.spacing.s16), verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s8)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = CalisTheme.typography.body1Bold, color = CalisTheme.colors.labelNormal, modifier = Modifier.weight(1f))
                Text(status, style = CalisTheme.typography.label1Medium, color = CalisTheme.colors.labelAlternative)
            }
            val modifier = Modifier.fillMaxWidth()
            if (progress == null) {
                LinearProgressIndicator(modifier, color = CalisTheme.colors.primaryNormal, trackColor = CalisTheme.colors.fillAlternative, strokeCap = StrokeCap.Round)
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = modifier,
                    color = CalisTheme.colors.primaryNormal,
                    trackColor = CalisTheme.colors.fillAlternative,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                )
            }
            detail?.let {
                Text(it, style = CalisTheme.typography.caption1Regular, color = CalisTheme.colors.labelAlternative, modifier = Modifier.testTag("analysis_remaining"))
            }
        }
    }
}

@Composable
private fun FailedContent(state: AnalysisUiState.Failed, onHome: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s12)) {
        Text(stringResource(R.string.analysis_failed_title), style = CalisTheme.typography.title3Bold, color = CalisTheme.colors.labelStrong)
        Text(stringResource(R.string.analysis_failed_body), style = CalisTheme.typography.body1ReadingRegular, color = CalisTheme.colors.labelNeutral)
        state.detail?.let {
            Text(it, style = CalisTheme.typography.caption1Regular, color = CalisTheme.colors.labelAssistive)
        }
    }
    Button(onClick = onHome, shape = RoundedCornerShape(CalisTheme.radius.component), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.analysis_failed_home), style = CalisTheme.typography.body1Bold)
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
