package com.calisvision.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalMontageColors = staticCompositionLocalOf { MontageLightColors }
private val LocalCalisColors = staticCompositionLocalOf { calisColorsOf(MontageLightColors, dark = false) }
private val LocalMontageTypography = staticCompositionLocalOf { MontageTypography() }

/** Accessors for the Montage-based tokens provided by [CalisVisionTheme]. */
object CalisTheme {
    val colors: MontageColors
        @Composable @ReadOnlyComposable get() = LocalMontageColors.current
    val appColors: CalisColors
        @Composable @ReadOnlyComposable get() = LocalCalisColors.current
    val typography: MontageTypography
        @Composable @ReadOnlyComposable get() = LocalMontageTypography.current
    val spacing: MontageSpacing get() = MontageSpacing
    val radius: MontageRadius get() = MontageRadius
    val shadow: MontageShadow get() = MontageShadow
}

private fun MontageColors.toMaterial(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primaryNormal,
        onPrimary = staticWhite,
        secondary = primaryNormal,
        onSecondary = staticWhite,
        tertiary = primaryNormal,
        onTertiary = staticWhite,
        background = backgroundNormalNormal,
        onBackground = labelNormal,
        surface = backgroundNormalNormal,
        onSurface = labelNormal,
        surfaceVariant = backgroundNormalAlternative,
        onSurfaceVariant = labelAlternative,
        surfaceContainerLowest = backgroundNormalNormal,
        surfaceContainerLow = backgroundElevatedNormal,
        surfaceContainer = backgroundElevatedNormal,
        surfaceContainerHigh = backgroundElevatedAlternative,
        surfaceContainerHighest = backgroundElevatedAlternative,
        inverseSurface = inverseBackground,
        inverseOnSurface = inverseLabel,
        inversePrimary = inversePrimary,
        error = statusNegative,
        onError = staticWhite,
        errorContainer = backgroundStatusNegative,
        onErrorContainer = statusNegative,
        outline = lineNormalNormal,
        outlineVariant = lineNormalAlternative,
        scrim = materialDimmer,
    )
}

private fun MontageTypography.toMaterial() = Typography(
    displayLarge = display1Bold,
    displayMedium = display2Bold,
    displaySmall = display3Bold,
    headlineLarge = title1Bold,
    headlineMedium = title2Bold,
    headlineSmall = title3Bold,
    titleLarge = heading1Bold,
    titleMedium = headline2Bold,
    titleSmall = label1Bold,
    bodyLarge = body1Regular,
    bodyMedium = body2Regular,
    bodySmall = caption1Regular,
    labelLarge = label1Medium,
    labelMedium = caption1Medium,
    labelSmall = caption2Medium,
)

private val MontageMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(MontageRadius.small),
    small = RoundedCornerShape(MontageRadius.small),
    medium = RoundedCornerShape(MontageRadius.large),
    large = RoundedCornerShape(MontageRadius.component),
    extraLarge = RoundedCornerShape(MontageRadius.component),
)

private val DefaultMontageTypography = MontageTypography()
private val MaterialTypography = DefaultMontageTypography.toMaterial()
private val MaterialLightColors = MontageLightColors.toMaterial(dark = false)
private val MaterialDarkColors = MontageDarkColors.toMaterial(dark = true)
private val CalisLightColors = calisColorsOf(MontageLightColors, dark = false)
private val CalisDarkColors = calisColorsOf(MontageDarkColors, dark = true)

/** Montage-token theme; follows system dark mode by default. */
@Composable
fun CalisVisionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMontageColors provides if (darkTheme) MontageDarkColors else MontageLightColors,
        LocalCalisColors provides if (darkTheme) CalisDarkColors else CalisLightColors,
        LocalMontageTypography provides DefaultMontageTypography,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) MaterialDarkColors else MaterialLightColors,
            typography = MaterialTypography,
            shapes = MontageMaterialShapes,
            content = content,
        )
    }
}
