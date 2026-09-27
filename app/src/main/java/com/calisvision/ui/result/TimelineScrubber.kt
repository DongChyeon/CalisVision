package com.calisvision.ui.result

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import com.calisvision.R
import com.calisvision.domain.analysis.FaultSegment
import com.calisvision.ui.theme.CalisTheme
import com.calisvision.ui.theme.MontageOpacity
import com.calisvision.video.FrameSource
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A fault band drawn over its full [fault] range, dimmed; [listed] is its hold-clipped, listed version (null when the
 * fault is not inside the hold), drawn solid and tappable over its range (user decision 2026-09-27).
 */
data class TimelineBand(val fault: FaultSegment, val listed: FaultSegment?)

/**
 * The track is split into [sampleCount] equal cells, one per sample: a tap or drag at x selects cell
 * ⌊x / width · sampleCount⌋, so moving one cell width moves exactly one sample (AC-7).
 * Each fault gets its own lane (in order of first appearance) so overlapping faults stay distinguishable.
 */
@Composable
fun TimelineScrubber(
    sampleCount: Int,
    sampleIndex: Int,
    hold: IntRange?,
    bands: List<TimelineBand>,
    onSeek: (Int) -> Unit,
    onBandTap: (FaultSegment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CalisTheme.colors
    val appColors = CalisTheme.appColors
    val spacing = CalisTheme.spacing
    val radius = CalisTheme.radius
    val currentBands by rememberUpdatedState(bands)
    val seek by rememberUpdatedState(onSeek)
    val bandTap by rememberUpdatedState(onBandTap)
    val description = stringResource(R.string.result_timeline_description)
    val last = (sampleCount - 1).coerceAtLeast(0)
    val lanes = bands.map { it.fault.faultId }.distinct()
    val laneHeight = spacing.s8
    val laneGap = spacing.s4
    val lanesHeight = laneGap + (laneHeight + laneGap) * lanes.size
    val trackHeight = maxOf(spacing.s32, lanesHeight)
    /** Top of lane 0; lanes are centred vertically. */
    val laneTop = (trackHeight - lanesHeight) / 2 + laneGap
    val currentLanes by rememberUpdatedState(lanes)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.s4)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .testTag("timeline")
                .semantics {
                    contentDescription = description
                    progressBarRangeInfo = ProgressBarRangeInfo(sampleIndex.toFloat(), 0f..last.toFloat(), steps = (last - 1).coerceAtLeast(0))
                    setProgress { target ->
                        seek(target.roundToInt().coerceIn(0, last))
                        true
                    }
                }
                .pointerInput(sampleCount) {
                    fun cell(x: Float) = sampleAt(x, size.width.toFloat(), sampleCount)
                    detectTapGestures { offset ->
                        val index = cell(offset.x)
                        seek(index)
                        val lane = currentLanes.getOrNull(floor((offset.y - (laneTop - laneGap / 2).toPx()) / (laneHeight + laneGap).toPx()).toInt())
                        currentBands.firstNotNullOfOrNull { band -> band.listed?.takeIf { it.faultId == lane && index in it.range } }?.let(bandTap)
                    }
                }
                .pointerInput(sampleCount) {
                    fun cell(x: Float) = sampleAt(x, size.width.toFloat(), sampleCount)
                    detectHorizontalDragGestures(onDragStart = { seek(cell(it.x)) }) { change, _ -> seek(cell(change.position.x)) }
                },
        ) {
            if (sampleCount == 0) return@Canvas
            val cellWidth = size.width / sampleCount
            fun span(range: IntRange, top: Float, height: Float) =
                Offset(range.first * cellWidth, top) to Size((range.last - range.first + 1) * cellWidth, height)

            drawRoundRect(colors.fillAlternative, cornerRadius = CornerRadius(radius.large.toPx()))
            hold?.let {
                val (o, s) = span(it, 0f, size.height)
                drawRect(appColors.holdSegmentHighlight, o, s)
                drawRect(colors.primaryNormal, o, Size(spacing.s1.toPx(), size.height))
                drawRect(colors.primaryNormal, Offset(o.x + s.width - spacing.s1.toPx(), 0f), Size(spacing.s1.toPx(), size.height))
            }
            bands.forEach { band ->
                val lane = lanes.indexOf(band.fault.faultId)
                val top = (laneTop + (laneHeight + laneGap) * lane).toPx()
                val (o, s) = span(band.fault.range, top, laneHeight.toPx())
                drawRoundRect(appColors.fault.copy(alpha = MontageOpacity.O28), o, s, CornerRadius(radius.small.toPx()))
                band.listed?.let {
                    val (lo, ls) = span(it.range, top, laneHeight.toPx())
                    drawRoundRect(appColors.fault, lo, ls, CornerRadius(radius.small.toPx()))
                }
            }
            val x = (sampleIndex + 0.5f) * cellWidth
            drawLine(colors.labelNormal, Offset(x, 0f), Offset(x, size.height), strokeWidth = spacing.s2.toPx())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val style = CalisTheme.typography.caption1Regular
            Text(stringResource(R.string.result_seconds, 0f), style = style, color = colors.labelAssistive)
            hold?.let {
                Text(
                    stringResource(R.string.result_hold_range, it.first * SAMPLE_SECONDS, it.last * SAMPLE_SECONDS),
                    style = style,
                    color = colors.primaryNormal,
                )
            }
            Text(stringResource(R.string.result_seconds, sampleCount * SAMPLE_SECONDS), style = style, color = colors.labelAssistive)
        }
    }
}

/** The sample whose cell contains [x] on a track [width] wide split into [sampleCount] equal cells; clamped to the track. */
fun sampleAt(x: Float, width: Float, sampleCount: Int): Int =
    if (sampleCount == 0 || width <= 0f) 0 else floor(x / width * sampleCount).toInt().coerceIn(0, sampleCount - 1)

/** Seconds per sample, for time labels. */
const val SAMPLE_SECONDS = FrameSource.SAMPLE_INTERVAL_MS / 1000f
