package com.calisvision.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calisvision.R
import com.calisvision.ui.components.CalisTopBar
import com.calisvision.ui.theme.CalisTheme

/** The frame takes at most this share of the window height so the timeline stays in view. */
private const val FRAME_MAX_HEIGHT_FRACTION = 0.45f

@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onBack: () -> Unit,
    onAnotherVideo: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val result = viewModel.result
    val frameMaxHeight = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * FRAME_MAX_HEIGHT_FRACTION).toDp() }

    Scaffold(
        topBar = { CalisTopBar(stringResource(R.string.result_title), onBack = onBack) },
        containerColor = CalisTheme.colors.backgroundNormalNormal,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CalisTheme.spacing.s20)
                .padding(bottom = CalisTheme.spacing.s24),
            verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s12),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (viewModel.sampleCount == 0) {
                Text(stringResource(R.string.result_no_frames), style = CalisTheme.typography.body1Regular, color = CalisTheme.colors.labelNeutral)
            } else {
                FrameWithSkeleton(
                    sampleIndex = state.sampleIndex,
                    aspect = result.aspect,
                    side = result.orientation?.side,
                    framePath = viewModel::framePath,
                    overlay = viewModel::overlay,
                    contentDescription = stringResource(R.string.result_frame_description, state.sampleIndex),
                    modifier = Modifier.heightIn(max = frameMaxHeight),
                )
                if (result.orientation == null) {
                    Text(stringResource(R.string.result_no_orientation), style = CalisTheme.typography.label1Regular, color = CalisTheme.colors.statusCautionary)
                }
                SampleStepper(
                    label = stringResource(R.string.result_time_sample, state.displayTimeMs / 1000f, state.sampleIndex),
                    onStep = viewModel::step,
                )
                TimelineScrubber(
                    sampleCount = viewModel.sampleCount,
                    sampleIndex = state.sampleIndex,
                    hold = result.holdSegment,
                    bands = state.faults.map { TimelineBand(it, it.isInHold(result.holdSegment)) },
                    onSeek = viewModel::seekTo,
                    onBandTap = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                if (result.holdSegment == null) {
                    Text(stringResource(R.string.result_no_hold), style = CalisTheme.typography.label1Regular, color = CalisTheme.colors.statusCautionary)
                }
            }
            OutlinedButton(
                onClick = onAnotherVideo,
                shape = RoundedCornerShape(CalisTheme.radius.component),
                border = BorderStroke(CalisTheme.spacing.divider, CalisTheme.colors.lineNormalNormal),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.result_another_video), style = CalisTheme.typography.body1Medium, color = CalisTheme.colors.labelNormal)
            }
        }
    }
}

@Composable
private fun SampleStepper(label: String, onStep: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onStep(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.result_prev_sample), tint = CalisTheme.colors.labelNormal)
        }
        Text(
            label,
            style = CalisTheme.typography.headline2Bold,
            color = CalisTheme.colors.labelNormal,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = { onStep(1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.result_next_sample), tint = CalisTheme.colors.labelNormal)
        }
    }
}
