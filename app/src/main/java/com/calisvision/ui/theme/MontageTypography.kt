/*
 * Type scale transcribed from Montage, Wanted's design system.
 * Source: https://github.com/wanteddev/montage-android @ v3.7.0 (commit e3b7813)
 *   - library/src/main/java/com/wanted/android/wanted/design/theme/Typography.kt
 * fontSize / lineHeight / letterSpacing / weights copied verbatim. Montage's "Bold" is
 * W700 for display1..title3 and W600 (SemiBold) for heading1..caption2, as upstream.
 *
 * Copyright (c) 2025 Wanted Lab, Inc. — MIT License (full text in MontageTokens.kt and
 * docs/THIRD_PARTY_NOTICES.md).
 *
 * Font: Pretendard Std 1.3.9 (SIL OFL 1.1), bundled unmodified under res/font.
 * Pretendard Std has no Hangul; Korean glyphs fall back to the system font.
 */
package com.calisvision.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.calisvision.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_std_regular, FontWeight.W400),
    Font(R.font.pretendard_std_medium, FontWeight.W500),
    Font(R.font.pretendard_std_semibold, FontWeight.W600),
    Font(R.font.pretendard_std_bold, FontWeight.W700),
)

private fun montageStyle(size: TextUnit, lineHeight: TextUnit, letterSpacing: TextUnit, weight: FontWeight) =
    TextStyle(
        fontFamily = Pretendard,
        fontSize = size,
        fontWeight = weight,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

private val Regular = FontWeight.W400
private val Medium = FontWeight.W500
private val SemiBold = FontWeight.W600
private val Bold = FontWeight.W700

@Immutable
data class MontageTypography(
    val display1Regular: TextStyle = montageStyle(56.sp, 72.sp, (-0.0319).em, Regular),
    val display1Medium: TextStyle = montageStyle(56.sp, 72.sp, (-0.0319).em, Medium),
    val display1Bold: TextStyle = montageStyle(56.sp, 72.sp, (-0.0319).em, Bold),
    val display2Regular: TextStyle = montageStyle(40.sp, 52.sp, (-0.0282).em, Regular),
    val display2Medium: TextStyle = montageStyle(40.sp, 52.sp, (-0.0282).em, Medium),
    val display2Bold: TextStyle = montageStyle(40.sp, 52.sp, (-0.0282).em, Bold),
    val display3Regular: TextStyle = montageStyle(36.sp, 48.sp, (-0.027).em, Regular),
    val display3Medium: TextStyle = montageStyle(36.sp, 48.sp, (-0.027).em, Medium),
    val display3Bold: TextStyle = montageStyle(36.sp, 48.sp, (-0.027).em, Bold),
    val title1Regular: TextStyle = montageStyle(32.sp, 44.sp, (-0.0253).em, Regular),
    val title1Medium: TextStyle = montageStyle(32.sp, 44.sp, (-0.0253).em, Medium),
    val title1Bold: TextStyle = montageStyle(32.sp, 44.sp, (-0.0253).em, Bold),
    val title2Regular: TextStyle = montageStyle(28.sp, 38.sp, (-0.0236).em, Regular),
    val title2Medium: TextStyle = montageStyle(28.sp, 38.sp, (-0.0236).em, Medium),
    val title2Bold: TextStyle = montageStyle(28.sp, 38.sp, (-0.0236).em, Bold),
    val title3Regular: TextStyle = montageStyle(24.sp, 32.sp, (-0.023).em, Regular),
    val title3Medium: TextStyle = montageStyle(24.sp, 32.sp, (-0.023).em, Medium),
    val title3Bold: TextStyle = montageStyle(24.sp, 32.sp, (-0.023).em, Bold),
    val heading1Regular: TextStyle = montageStyle(22.sp, 30.sp, (-0.0194).em, Regular),
    val heading1Medium: TextStyle = montageStyle(22.sp, 30.sp, (-0.0194).em, Medium),
    val heading1Bold: TextStyle = montageStyle(22.sp, 30.sp, (-0.0194).em, SemiBold),
    val heading2Regular: TextStyle = montageStyle(20.sp, 28.sp, (-0.012).em, Regular),
    val heading2Medium: TextStyle = montageStyle(20.sp, 28.sp, (-0.012).em, Medium),
    val heading2Bold: TextStyle = montageStyle(20.sp, 28.sp, (-0.012).em, SemiBold),
    val headline1Regular: TextStyle = montageStyle(18.sp, 26.sp, (-0.002).em, Regular),
    val headline1Medium: TextStyle = montageStyle(18.sp, 26.sp, (-0.002).em, Medium),
    val headline1Bold: TextStyle = montageStyle(18.sp, 26.sp, (-0.002).em, SemiBold),
    val headline2Regular: TextStyle = montageStyle(17.sp, 24.sp, 0.em, Regular),
    val headline2Medium: TextStyle = montageStyle(17.sp, 24.sp, 0.em, Medium),
    val headline2Bold: TextStyle = montageStyle(17.sp, 24.sp, 0.em, SemiBold),
    val body1Regular: TextStyle = montageStyle(16.sp, 24.sp, 0.0057.em, Regular),
    val body1Medium: TextStyle = montageStyle(16.sp, 24.sp, 0.0057.em, Medium),
    val body1Bold: TextStyle = montageStyle(16.sp, 24.sp, 0.0057.em, SemiBold),
    val body1ReadingRegular: TextStyle = montageStyle(16.sp, 26.sp, 0.0057.em, Regular),
    val body1ReadingMedium: TextStyle = montageStyle(16.sp, 26.sp, 0.0057.em, Medium),
    val body1ReadingBold: TextStyle = montageStyle(16.sp, 26.sp, 0.0057.em, SemiBold),
    val body2Regular: TextStyle = montageStyle(15.sp, 22.sp, 0.0096.em, Regular),
    val body2Medium: TextStyle = montageStyle(15.sp, 22.sp, 0.0096.em, Medium),
    val body2Bold: TextStyle = montageStyle(15.sp, 22.sp, 0.0096.em, SemiBold),
    val body2ReadingRegular: TextStyle = montageStyle(15.sp, 24.sp, 0.0096.em, Regular),
    val body2ReadingMedium: TextStyle = montageStyle(15.sp, 24.sp, 0.0096.em, Medium),
    val body2ReadingBold: TextStyle = montageStyle(15.sp, 24.sp, 0.0096.em, SemiBold),
    val label1Regular: TextStyle = montageStyle(14.sp, 20.sp, 0.0145.em, Regular),
    val label1Medium: TextStyle = montageStyle(14.sp, 20.sp, 0.0145.em, Medium),
    val label1Bold: TextStyle = montageStyle(14.sp, 20.sp, 0.0145.em, SemiBold),
    val label1ReadingRegular: TextStyle = montageStyle(14.sp, 22.sp, 0.0145.em, Regular),
    val label1ReadingMedium: TextStyle = montageStyle(14.sp, 22.sp, 0.0145.em, Medium),
    val label1ReadingBold: TextStyle = montageStyle(14.sp, 22.sp, 0.0145.em, SemiBold),
    val label2Regular: TextStyle = montageStyle(13.sp, 18.sp, 0.0194.em, Regular),
    val label2Medium: TextStyle = montageStyle(13.sp, 18.sp, 0.0194.em, Medium),
    val label2Bold: TextStyle = montageStyle(13.sp, 18.sp, 0.0194.em, SemiBold),
    val caption1Regular: TextStyle = montageStyle(12.sp, 16.sp, 0.0252.em, Regular),
    val caption1Medium: TextStyle = montageStyle(12.sp, 16.sp, 0.0252.em, Medium),
    val caption1Bold: TextStyle = montageStyle(12.sp, 16.sp, 0.0252.em, SemiBold),
    val caption2Regular: TextStyle = montageStyle(11.sp, 14.sp, 0.0311.em, Regular),
    val caption2Medium: TextStyle = montageStyle(11.sp, 14.sp, 0.0311.em, Medium),
    val caption2Bold: TextStyle = montageStyle(11.sp, 14.sp, 0.0311.em, SemiBold),
)
