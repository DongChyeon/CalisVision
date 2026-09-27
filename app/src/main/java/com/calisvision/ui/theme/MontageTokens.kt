/*
 * Design tokens transcribed from Montage, Wanted's design system.
 * Source: https://github.com/wanteddev/montage-android @ v3.7.0 (commit e3b7813)
 *   - library/src/main/res/values/design_system_atomic_colors.xml       -> MontageAtomic
 *   - library/src/main/res/values/design_system_semantic_colors.xml     -> MontageLightColors
 *   - library/src/main/res/values-night/design_system_semantic_colors.xml -> MontageDarkColors
 *   - library/src/main/res/values/dimens.xml                            -> MontageSpacing, MontageOpacity
 *   - library/src/main/java/.../design/theme/Shape.kt                   -> MontageRadius
 *   - library/src/main/java/.../design/base/WantedDropShadow.kt         -> MontageShadow
 * Values are copied verbatim; only names were converted to Kotlin camelCase.
 * `*_temp` duplicates were omitted. The per-role `*_opacityNN` helper colors are not
 * transcribed except where CalisColors references them.
 *
 * MIT License
 *
 * Copyright (c) 2025 Wanted Lab, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.calisvision.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Montage atomic palette (design_system_atomic_colors.xml). */
object MontageAtomic {

    // Common
    val transparent = Color(0x00000000)
    val common100 = Color(0xFFFFFFFF)
    val common0 = Color(0xFF000000)

    // Neutral
    val neutral99 = Color(0xFFF7F7F7)
    val neutral95 = Color(0xFFDCDCDC)
    val neutral90 = Color(0xFFC4C4C4)
    val neutral80 = Color(0xFFB0B0B0)
    val neutral70 = Color(0xFF9B9B9B)
    val neutral60 = Color(0xFF8A8A8A)
    val neutral50 = Color(0xFF737373)
    val neutral40 = Color(0xFF5C5C5C)
    val neutral30 = Color(0xFF474747)
    val neutral22 = Color(0xFF303030)
    val neutral20 = Color(0xFF2A2A2A)
    val neutral15 = Color(0xFF1C1C1C)
    val neutral10 = Color(0xFF171717)
    val neutral5 = Color(0xFF0F0F0F)

    // Cool Neutral
    val coolNeutral99 = Color(0xFFF7F7F8)
    val coolNeutral98 = Color(0xFFF4F4F5)
    val coolNeutral97 = Color(0xFFEAEBEC)
    val coolNeutral96 = Color(0xFFE1E2E4)
    val coolNeutral95 = Color(0xFFDBDCDF)
    val coolNeutral90 = Color(0xFFC2C4C8)
    val coolNeutral80 = Color(0xFFAEB0B6)
    val coolNeutral70 = Color(0xFF989BA2)
    val coolNeutral60 = Color(0xFF878A93)
    val coolNeutral50 = Color(0xFF70737C)
    val coolNeutral40 = Color(0xFF5A5C63)
    val coolNeutral30 = Color(0xFF46474C)
    val coolNeutral25 = Color(0xFF37383C)
    val coolNeutral23 = Color(0xFF333438)
    val coolNeutral22 = Color(0xFF2E2F33)
    val coolNeutral20 = Color(0xFF292A2D)
    val coolNeutral17 = Color(0xFF212225)
    val coolNeutral15 = Color(0xFF1B1C1E)
    val coolNeutral10 = Color(0xFF171719)
    val coolNeutral7 = Color(0xFF141415)
    val coolNeutral5 = Color(0xFF0F0F10)

    // Blue
    val blue99 = Color(0xFFF7FBFF)
    val blue95 = Color(0xFFEAF2FE)
    val blue90 = Color(0xFFC9DEFE)
    val blue80 = Color(0xFF9EC5FF)
    val blue70 = Color(0xFF69A5FF)
    val blue65 = Color(0xFF4F95FF)
    val blue60 = Color(0xFF3385FF)
    val blue55 = Color(0xFF1A75FF)
    val blue50 = Color(0xFF0066FF)
    val blue45 = Color(0xFF005EEB)
    val blue40 = Color(0xFF0054D1)
    val blue30 = Color(0xFF003E9C)
    val blue20 = Color(0xFF002966)
    val blue10 = Color(0xFF001536)

    // Red
    val red99 = Color(0xFFFFFAFA)
    val red95 = Color(0xFFFEECEC)
    val red90 = Color(0xFFFED5D5)
    val red80 = Color(0xFFFFB5B5)
    val red70 = Color(0xFFFF8C8C)
    val red60 = Color(0xFFFF6363)
    val red50 = Color(0xFFFF4242)
    val red40 = Color(0xFFE52222)
    val red30 = Color(0xFFB00C0C)
    val red20 = Color(0xFF730303)
    val red10 = Color(0xFF3B0101)

    // Green
    val green99 = Color(0xFFF2FFF6)
    val green95 = Color(0xFFD9FFE6)
    val green90 = Color(0xFFACFCC7)
    val green80 = Color(0xFF7DF5A5)
    val green70 = Color(0xFF49E57D)
    val green60 = Color(0xFF1ED45A)
    val green50 = Color(0xFF00BF40)
    val green40 = Color(0xFF009632)
    val green30 = Color(0xFF006E25)
    val green20 = Color(0xFF004517)
    val green10 = Color(0xFF00240C)

    // Orange
    val orange99 = Color(0xFFFFFCF7)
    val orange95 = Color(0xFFFEF4E6)
    val orange90 = Color(0xFFFEE6C6)
    val orange80 = Color(0xFFFFD49C)
    val orange70 = Color(0xFFFFC06E)
    val orange60 = Color(0xFFFFA938)
    val orange50 = Color(0xFFFF9200)
    val orange40 = Color(0xFFD47800)
    val orange39 = Color(0xFFD17600)
    val orange30 = Color(0xFF9C5800)
    val orange20 = Color(0xFF663A00)
    val orange10 = Color(0xFF361E00)

    // Red Orange
    val redorange99 = Color(0xFFFFFAF7)
    val redorange95 = Color(0xFFFEEEE5)
    val redorange90 = Color(0xFFFED9C4)
    val redorange80 = Color(0xFFFFBD96)
    val redorange70 = Color(0xFFFF9B61)
    val redorange60 = Color(0xFFFF7B2E)
    val redorange50 = Color(0xFFFF5E00)
    val redorange48 = Color(0xFFF55A00)
    val redorange40 = Color(0xFFC94A00)
    val redorange30 = Color(0xFF913500)
    val redorange20 = Color(0xFF592100)
    val redorange10 = Color(0xFF290F00)

    // Lime
    val lime99 = Color(0xFFF8FFF2)
    val lime95 = Color(0xFFE6FFD4)
    val lime90 = Color(0xFFCCFCA9)
    val lime80 = Color(0xFFAEF779)
    val lime70 = Color(0xFF88F03E)
    val lime60 = Color(0xFF6BE016)
    val lime50 = Color(0xFF58CF04)
    val lime40 = Color(0xFF48AD00)
    val lime37 = Color(0xFF429E00)
    val lime30 = Color(0xFF347D00)
    val lime20 = Color(0xFF225200)
    val lime10 = Color(0xFF112900)

    // Cyan
    val cyan99 = Color(0xFFF7FEFF)
    val cyan95 = Color(0xFFDEFAFF)
    val cyan90 = Color(0xFFB5F4FF)
    val cyan80 = Color(0xFF8AEDFF)
    val cyan70 = Color(0xFF57DFF7)
    val cyan60 = Color(0xFF28D0ED)
    val cyan50 = Color(0xFF00BDDE)
    val cyan40 = Color(0xFF0098B2)
    val cyan30 = Color(0xFF006F82)
    val cyan20 = Color(0xFF004854)
    val cyan10 = Color(0xFF00252B)

    // Light Blue
    val lightblue99 = Color(0xFFF7FDFF)
    val lightblue95 = Color(0xFFE5F6FE)
    val lightblue90 = Color(0xFFC4ECFE)
    val lightblue80 = Color(0xFFA1E1FF)
    val lightblue70 = Color(0xFF70D2FF)
    val lightblue60 = Color(0xFF3DC2FF)
    val lightblue50 = Color(0xFF00AEFF)
    val lightblue40 = Color(0xFF008DCF)
    val lightblue30 = Color(0xFF006796)
    val lightblue20 = Color(0xFF004261)
    val lightblue10 = Color(0xFF002130)

    // Violet
    val violet99 = Color(0xFFFBFAFF)
    val violet95 = Color(0xFFF0ECFE)
    val violet90 = Color(0xFFDBD3FE)
    val violet80 = Color(0xFFC0B0FF)
    val violet70 = Color(0xFF9E86FC)
    val violet60 = Color(0xFF7D5EF7)
    val violet50 = Color(0xFF6541F2)
    val violet45 = Color(0xFF5B37ED)
    val violet40 = Color(0xFF4F29E5)
    val violet30 = Color(0xFF3A16C9)
    val violet20 = Color(0xFF23098F)
    val violet10 = Color(0xFF11024D)

    // Purple
    val purple99 = Color(0xFFFEFBFF)
    val purple95 = Color(0xFFF9EDFF)
    val purple90 = Color(0xFFF2D6FF)
    val purple80 = Color(0xFFE9BAFF)
    val purple70 = Color(0xFFDE96FF)
    val purple60 = Color(0xFFD478FF)
    val purple50 = Color(0xFFCB59FF)
    val purple40 = Color(0xFFAD36E3)
    val purple30 = Color(0xFF861CB8)
    val purple20 = Color(0xFF580A7D)
    val purple10 = Color(0xFF290247)

    // Pink
    val pink99 = Color(0xFFFFFAFE)
    val pink95 = Color(0xFFFEECFB)
    val pink90 = Color(0xFFFED3F7)
    val pink80 = Color(0xFFFFB8F3)
    val pink70 = Color(0xFFFF94ED)
    val pink60 = Color(0xFFFA73E3)
    val pink50 = Color(0xFFF553DA)
    val pink46 = Color(0xFFE846CD)
    val pink40 = Color(0xFFD331B8)
    val pink30 = Color(0xFFA81690)
    val pink20 = Color(0xFF730560)
    val pink10 = Color(0xFF3D0133)

    // Opacity Colors
    val coolNeutral90Opacity88 = Color(0xE0C2C4C8)
    val coolNeutral80Opacity61 = Color(0x9CAEB0B6)
    val coolNeutral80Opacity28 = Color(0x47AEB0B6)
    val coolNeutral70Opacity16 = Color(0x29989BA2)
    val coolNeutral50Opacity8 = Color(0x1470737C)
    val coolNeutral50Opacity16 = Color(0x2970737C)
    val coolNeutral50Opacity22 = Color(0x3870737C)
    val coolNeutral50Opacity28 = Color(0x4770737C)
    val coolNeutral50Opacity32 = Color(0x5270737C)
    val coolNeutral25Opacity16 = Color(0x2937383C)
    val coolNeutral25Opacity28 = Color(0x4737383C)
    val coolNeutral25Opacity61 = Color(0x9C37383C)
    val coolNeutral22Opacity88 = Color(0xE02E2F33)
    val coolNeutral17Opacity61 = Color(0x9C212225)
    val red50Opacity8 = Color(0x14FF4242)
    val red60Opacity8 = Color(0x14FF6363)
    val orange50Opacity8 = Color(0x14FF9200)
    val orange60Opacity8 = Color(0x14FFA938)
    val green50Opacity8 = Color(0x1400BF40)
    val green60Opacity8 = Color(0x141ED45A)
    val orange50Opacity43 = Color(0x6EFF9200)
    val orange60Opacity43 = Color(0x6EFFA938)
    val green50Opacity43 = Color(0x6E00BF40)
    val green60Opacity43 = Color(0x6E1ED45A)
    val common100Opacity8 = Color(0x14FFFFFF)
    val common100Opacity28 = Color(0x47FFFFFF)
}

/** Montage semantic color roles; one instance per theme (light / night resources). */
@Immutable
data class MontageColors(
    val staticWhite: Color,
    val staticBlack: Color,
    val primaryNormal: Color,
    val primaryStrong: Color,
    val primaryHeavy: Color,
    val labelNormal: Color,
    val labelStrong: Color,
    val labelNeutral: Color,
    val labelAlternative: Color,
    val labelAssistive: Color,
    val labelDisable: Color,
    val backgroundNormalNormal: Color,
    val backgroundNormalAlternative: Color,
    val backgroundElevatedNormal: Color,
    val backgroundElevatedAlternative: Color,
    val backgroundTransparentNormal: Color,
    val backgroundTransparentAlternative: Color,
    val backgroundStatusNegative: Color,
    val backgroundStatusCautionary: Color,
    val backgroundStatusPositive: Color,
    val interactionInactive: Color,
    val interactionDisable: Color,
    val lineNormalNormal: Color,
    val lineNormalNeutral: Color,
    val lineNormalAlternative: Color,
    val lineSolidNormal: Color,
    val lineSolidNeutral: Color,
    val lineSolidAlternative: Color,
    val lineStatusCautionaryNormal: Color,
    val lineStatusPositiveNormal: Color,
    val statusPositive: Color,
    val statusCautionary: Color,
    val statusNegative: Color,
    val accentBackgroundLime: Color,
    val accentBackgroundCyan: Color,
    val accentBackgroundLightblue: Color,
    val accentBackgroundViolet: Color,
    val accentBackgroundPurple: Color,
    val accentBackgroundPink: Color,
    val accentBackgroundRedorange: Color,
    val accentForegroundRed: Color,
    val accentForegroundRedorange: Color,
    val accentForegroundOrange: Color,
    val accentForegroundLime: Color,
    val accentForegroundGreen: Color,
    val accentForegroundCyan: Color,
    val accentForegroundLightblue: Color,
    val accentForegroundBlue: Color,
    val accentForegroundViolet: Color,
    val accentForegroundPurple: Color,
    val accentForegroundPink: Color,
    val inversePrimary: Color,
    val inverseBackground: Color,
    val inverseLabel: Color,
    val fillNormal: Color,
    val fillStrong: Color,
    val fillAlternative: Color,
    val materialDimmer: Color,
)

val MontageLightColors = MontageColors(
    staticWhite = MontageAtomic.common100,
    staticBlack = MontageAtomic.common0,
    primaryNormal = MontageAtomic.blue50,
    primaryStrong = MontageAtomic.blue45,
    primaryHeavy = MontageAtomic.blue40,
    labelNormal = MontageAtomic.coolNeutral10,
    labelStrong = MontageAtomic.common0,
    labelNeutral = MontageAtomic.coolNeutral22Opacity88,
    labelAlternative = MontageAtomic.coolNeutral25Opacity61,
    labelAssistive = MontageAtomic.coolNeutral25Opacity28,
    labelDisable = MontageAtomic.coolNeutral25Opacity16,
    backgroundNormalNormal = MontageAtomic.common100,
    backgroundNormalAlternative = MontageAtomic.coolNeutral99,
    backgroundElevatedNormal = MontageAtomic.common100,
    backgroundElevatedAlternative = MontageAtomic.coolNeutral99,
    backgroundTransparentNormal = MontageAtomic.common100Opacity8,
    backgroundTransparentAlternative = MontageAtomic.common100Opacity28,
    backgroundStatusNegative = MontageAtomic.red50Opacity8,
    backgroundStatusCautionary = MontageAtomic.orange50Opacity8,
    backgroundStatusPositive = MontageAtomic.green50Opacity8,
    interactionInactive = MontageAtomic.coolNeutral70,
    interactionDisable = MontageAtomic.coolNeutral98,
    lineNormalNormal = MontageAtomic.coolNeutral50Opacity22,
    lineNormalNeutral = MontageAtomic.coolNeutral50Opacity16,
    lineNormalAlternative = MontageAtomic.coolNeutral50Opacity8,
    lineSolidNormal = MontageAtomic.coolNeutral96,
    lineSolidNeutral = MontageAtomic.coolNeutral97,
    lineSolidAlternative = MontageAtomic.coolNeutral98,
    lineStatusCautionaryNormal = MontageAtomic.orange50Opacity43,
    lineStatusPositiveNormal = MontageAtomic.green50Opacity43,
    statusPositive = MontageAtomic.green50,
    statusCautionary = MontageAtomic.orange50,
    statusNegative = MontageAtomic.red50,
    accentBackgroundLime = MontageAtomic.lime50,
    accentBackgroundCyan = MontageAtomic.cyan50,
    accentBackgroundLightblue = MontageAtomic.lightblue50,
    accentBackgroundViolet = MontageAtomic.violet50,
    accentBackgroundPurple = MontageAtomic.purple50,
    accentBackgroundPink = MontageAtomic.pink50,
    accentBackgroundRedorange = MontageAtomic.redorange50,
    accentForegroundRed = MontageAtomic.red40,
    accentForegroundRedorange = MontageAtomic.redorange48,
    accentForegroundOrange = MontageAtomic.orange39,
    accentForegroundLime = MontageAtomic.lime37,
    accentForegroundGreen = MontageAtomic.green40,
    accentForegroundCyan = MontageAtomic.cyan40,
    accentForegroundLightblue = MontageAtomic.lightblue40,
    accentForegroundBlue = MontageAtomic.blue45,
    accentForegroundViolet = MontageAtomic.violet45,
    accentForegroundPurple = MontageAtomic.purple40,
    accentForegroundPink = MontageAtomic.pink46,
    inversePrimary = MontageAtomic.blue60,
    inverseBackground = MontageAtomic.coolNeutral15,
    inverseLabel = MontageAtomic.coolNeutral99,
    fillNormal = Color(0x1470737C),
    fillStrong = Color(0x2970737C),
    fillAlternative = Color(0x0D70737C),
    materialDimmer = Color(0x85171719),
)

val MontageDarkColors = MontageColors(
    staticWhite = MontageAtomic.common100,
    staticBlack = MontageAtomic.common0,
    primaryNormal = MontageAtomic.blue60,
    primaryStrong = MontageAtomic.blue55,
    primaryHeavy = MontageAtomic.blue50,
    labelNormal = MontageAtomic.coolNeutral99,
    labelStrong = MontageAtomic.common100,
    labelNeutral = MontageAtomic.coolNeutral90Opacity88,
    labelAlternative = MontageAtomic.coolNeutral80Opacity61,
    labelAssistive = MontageAtomic.coolNeutral80Opacity28,
    labelDisable = MontageAtomic.coolNeutral70Opacity16,
    backgroundNormalNormal = MontageAtomic.coolNeutral15,
    backgroundNormalAlternative = MontageAtomic.coolNeutral5,
    backgroundElevatedNormal = MontageAtomic.coolNeutral17,
    backgroundElevatedAlternative = MontageAtomic.coolNeutral7,
    backgroundTransparentNormal = MontageAtomic.coolNeutral17Opacity61,
    backgroundTransparentAlternative = MontageAtomic.coolNeutral17Opacity61,
    backgroundStatusNegative = MontageAtomic.red60Opacity8,
    backgroundStatusCautionary = MontageAtomic.orange60Opacity8,
    backgroundStatusPositive = MontageAtomic.green60Opacity8,
    interactionInactive = MontageAtomic.coolNeutral40,
    interactionDisable = MontageAtomic.coolNeutral22,
    lineNormalNormal = MontageAtomic.coolNeutral50Opacity32,
    lineNormalNeutral = MontageAtomic.coolNeutral50Opacity28,
    lineNormalAlternative = MontageAtomic.coolNeutral50Opacity22,
    lineSolidNormal = MontageAtomic.coolNeutral25,
    lineSolidNeutral = MontageAtomic.coolNeutral23,
    lineSolidAlternative = MontageAtomic.coolNeutral22,
    lineStatusCautionaryNormal = MontageAtomic.orange60Opacity43,
    lineStatusPositiveNormal = MontageAtomic.green60Opacity43,
    statusPositive = MontageAtomic.green60,
    statusCautionary = MontageAtomic.orange60,
    statusNegative = MontageAtomic.red60,
    accentBackgroundLime = MontageAtomic.lime60,
    accentBackgroundCyan = MontageAtomic.cyan60,
    accentBackgroundLightblue = MontageAtomic.lightblue60,
    accentBackgroundViolet = MontageAtomic.violet60,
    accentBackgroundPurple = MontageAtomic.purple60,
    accentBackgroundPink = MontageAtomic.pink60,
    accentBackgroundRedorange = MontageAtomic.redorange60,
    accentForegroundRed = MontageAtomic.red60,
    accentForegroundRedorange = MontageAtomic.redorange60,
    accentForegroundOrange = MontageAtomic.orange50,
    accentForegroundLime = MontageAtomic.lime50,
    accentForegroundGreen = MontageAtomic.green60,
    accentForegroundCyan = MontageAtomic.cyan50,
    accentForegroundLightblue = MontageAtomic.lightblue50,
    accentForegroundBlue = MontageAtomic.blue65,
    accentForegroundViolet = MontageAtomic.violet70,
    accentForegroundPurple = MontageAtomic.purple60,
    accentForegroundPink = MontageAtomic.pink60,
    inversePrimary = MontageAtomic.blue50,
    inverseBackground = MontageAtomic.common100,
    inverseLabel = MontageAtomic.neutral10,
    fillNormal = Color(0x3870737C),
    fillStrong = Color(0x4770737C),
    fillAlternative = Color(0x1F70737C),
    materialDimmer = Color(0x74171719),
)

/** Montage opacity scale (dimens.xml `opacity_*`). */
object MontageOpacity {
    const val O0 = 0f
    const val O5 = 0.05f
    const val O8 = 0.08f
    const val O12 = 0.12f
    const val O16 = 0.16f
    const val O22 = 0.22f
    const val O28 = 0.28f
    const val O35 = 0.35f
    const val O43 = 0.43f
    const val O52 = 0.52f
    const val O61 = 0.61f
    const val O74 = 0.74f
    const val O88 = 0.88f
    const val O97 = 0.97f
    const val O100 = 1f
}

/** Montage margin/padding scale (dimens.xml `margin_*` / `padding_*`, identical values). */
@Immutable
object MontageSpacing {
    val s1: Dp = 1.dp
    val s2: Dp = 2.dp
    val s4: Dp = 4.dp
    val s8: Dp = 8.dp
    val s12: Dp = 12.dp
    val s16: Dp = 16.dp
    val s20: Dp = 20.dp
    val s24: Dp = 24.dp
    val s28: Dp = 28.dp
    val s32: Dp = 32.dp
    val s36: Dp = 36.dp
    val s40: Dp = 40.dp
    val s44: Dp = 44.dp
    val s48: Dp = 48.dp
    val divider: Dp = 1.dp
}

/**
 * Corner radii. Montage v3.7.0 ships no dedicated radius token file; these are the values
 * it actually uses: Shape.kt (small/medium 4dp, large 8dp) and the 12dp default
 * `borderRadius` of every WantedShadowStyle (also the most frequent component radius).
 */
@Immutable
object MontageRadius {
    val small: Dp = 4.dp
    val medium: Dp = 4.dp
    val large: Dp = 8.dp
    val component: Dp = 12.dp
}

/** One layer of a Montage drop shadow (CSS-style box-shadow). */
@Immutable
data class MontageShadowLayer(
    val offsetX: Dp,
    val offsetY: Dp,
    val blurRadius: Dp,
    val spreadRadius: Dp,
    val color: Color,
)

/**
 * Montage shadow presets (WantedDropShadowDefaults.WantedShadowStyle) and the legacy
 * elevation dimens (dimens.xml `shadow_*`).
 */
object MontageShadow {
    val xSmall = listOf(
        MontageShadowLayer(0.dp, 1.dp, 0.87.dp, (-1).dp, Color(0x1A171717)),
    )
    val small = listOf(
        MontageShadowLayer(0.dp, 2.dp, 1.73.dp, (-2).dp, Color(0x0F171717)),
        MontageShadowLayer(0.dp, 4.dp, 4.33.dp, (-1).dp, Color(0x0F171717)),
    )
    val medium = listOf(
        MontageShadowLayer(0.dp, 4.dp, 3.46.dp, (-2).dp, Color(0x12000000)),
        MontageShadowLayer(0.dp, 10.dp, 10.39.dp, (-3).dp, Color(0x12171717)),
    )
    val large = listOf(
        MontageShadowLayer(0.dp, 6.dp, 5.20.dp, (-4).dp, Color(0x14171717)),
        MontageShadowLayer(0.dp, 16.dp, 15.59.dp, (-6).dp, Color(0x14171717)),
    )
    val xLarge = listOf(
        MontageShadowLayer(0.dp, 10.dp, 8.66.dp, (-5).dp, Color(0x1A171717)),
        MontageShadowLayer(0.dp, 24.dp, 24.25.dp, (-10).dp, Color(0x1F171717)),
    )

    val elevationNormal: Dp = 1.dp
    val elevationEmphasize: Dp = 3.dp
    val elevationStrong: Dp = 6.dp
    val elevationHeavy: Dp = 12.dp
}
