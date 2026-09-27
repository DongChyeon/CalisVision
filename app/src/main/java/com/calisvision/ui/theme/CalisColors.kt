package com.calisvision.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * CalisVision-specific roles layered on Montage semantic tokens (ADR-0007).
 * Every value resolves to an existing Montage token; nothing here is a new color.
 */
@Immutable
data class CalisColors(
    /** Pose within tolerance / joint in normal range. Montage status.positive. */
    val poseOk: Color,
    val jointNormal: Color,
    /** Detected fault (e.g. shoulder angle out of range). Montage status.negative. */
    val fault: Color,
    val faultBackground: Color,
    /** Borderline value, needs attention. Montage status.cautionary. */
    val warning: Color,
    val warningBackground: Color,
    /** Hold-segment band on timelines/charts. Montage primary_normal_opacity22. */
    val holdSegmentHighlight: Color,
    /**
     * Skeleton bones drawn over video frames. The frame is not themed, so both modes use
     * Montage static_white_opacity88.
     */
    val skeletonLine: Color,
)

internal fun calisColorsOf(montage: MontageColors, dark: Boolean) = CalisColors(
    poseOk = montage.statusPositive,
    jointNormal = montage.statusPositive,
    fault = montage.statusNegative,
    faultBackground = montage.backgroundStatusNegative,
    warning = montage.statusCautionary,
    warningBackground = montage.backgroundStatusCautionary,
    // primary_normal_opacity22: light #383366FF, night #385B84FF (semantic_colors.xml).
    holdSegmentHighlight = if (dark) Color(0x385B84FF) else Color(0x383366FF),
    // static_white_opacity88: #E0FFFFFF in both light and night semantic_colors.xml.
    skeletonLine = Color(0xE0FFFFFF),
)
