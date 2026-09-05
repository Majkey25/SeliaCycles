package com.majkeylab.seliacycles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val seliaLightColors = lightColorScheme(
    primary = Color(0xFF775900),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE08A),
    onPrimaryContainer = Color(0xFF3D2B00),
    secondary = Color(0xFFC62828),
    secondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF00695C),
    tertiaryContainer = Color(0xFFA7F3E8),
    background = Color(0xFFFFF9F7),
    surface = Color(0xFFFFF9F7),
    surfaceVariant = Color(0xFFF2E3E0),
    outline = Color(0xFF79747E),
)

private val seliaDarkColors = darkColorScheme(
    primary = Color(0xFFFFD54F),
    onPrimary = Color(0xFF3A2A00),
    primaryContainer = Color(0xFF5B4300),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFFFFB4AB),
    secondaryContainer = Color(0xFF93000A),
    tertiary = Color(0xFF65D8C8),
    tertiaryContainer = Color(0xFF005047),
    background = Color(0xFF191210),
    surface = Color(0xFF191210),
    surfaceVariant = Color(0xFF3A2E2C),
    outline = Color(0xFF938F99),
)

private val roseLightColors = lightColorScheme(
    primary = Color(0xFF8F2957),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E3),
    onPrimaryContainer = Color(0xFF3B071F),
    secondary = Color(0xFF75565F),
    secondaryContainer = Color(0xFFFFD9E3),
    tertiary = Color(0xFF7A5730),
    tertiaryContainer = Color(0xFFFFDDB5),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFF8F8),
    surfaceVariant = Color(0xFFF3DDE2),
    outline = Color(0xFF837377),
)

private val roseDarkColors = darkColorScheme(
    primary = Color(0xFFFFB0C8),
    onPrimary = Color(0xFF56102F),
    primaryContainer = Color(0xFF72203F),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFE4BDC6),
    secondaryContainer = Color(0xFF5C3F47),
    tertiary = Color(0xFFEABD8C),
    tertiaryContainer = Color(0xFF60401B),
    background = Color(0xFF1A1114),
    surface = Color(0xFF1A1114),
    surfaceVariant = Color(0xFF514347),
    outline = Color(0xFF9E8C91),
)

private val oceanLightColors = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF4D616C),
    secondaryContainer = Color(0xFFD0E6F2),
    tertiary = Color(0xFF006C4C),
    tertiaryContainer = Color(0xFF89F8C7),
    background = Color(0xFFF8F9FF),
    surface = Color(0xFFF8F9FF),
    surfaceVariant = Color(0xFFDFE2EB),
    outline = Color(0xFF73777F),
)

private val oceanDarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFB4CAD6),
    secondaryContainer = Color(0xFF354A53),
    tertiary = Color(0xFF6CDBAC),
    tertiaryContainer = Color(0xFF005138),
    background = Color(0xFF101418),
    surface = Color(0xFF101418),
    surfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF8C9199),
)

private data class PaletteRgb(val primary: Int, val secondary: Int, val tertiary: Int)

private fun paletteRgb(palette: AppPalette, custom: CustomPalette): PaletteRgb = when (palette) {
    AppPalette.SELIA -> PaletteRgb(0xF4B400, 0xC62828, 0x00897B)
    AppPalette.ROSE -> PaletteRgb(0x8F2957, 0x75565F, 0x7A5730)
    AppPalette.OCEAN -> PaletteRgb(0x0061A4, 0x4D616C, 0x006C4C)
    AppPalette.FOREST -> PaletteRgb(0x2F6846, 0x52634F, 0x47664B)
    AppPalette.SUNSET -> PaletteRgb(0x9C432E, 0x8B4A5C, 0x7C5800)
    AppPalette.LILAC -> PaletteRgb(0x6F4BA7, 0x7B526B, 0x4C6090)
    AppPalette.CUSTOM -> PaletteRgb(custom.primaryRgb, custom.secondaryRgb, custom.tertiaryRgb)
}

internal fun palettePreviewColors(
    palette: AppPalette,
    custom: CustomPalette = CustomPalette(),
): List<Color> = paletteRgb(palette, custom).let { listOf(it.primary.color(), it.secondary.color(), it.tertiary.color()) }

internal fun paletteAsCustom(palette: AppPalette): CustomPalette {
    require(palette != AppPalette.CUSTOM)
    return paletteRgb(palette, CustomPalette()).let { CustomPalette(it.primary, it.secondary, it.tertiary) }
}

internal fun paletteGradientColors(
    palette: AppPalette,
    custom: CustomPalette = CustomPalette(),
): List<Color> = paletteRgb(palette, custom).let {
    listOf(it.primary.color().withReadableWhite(), it.secondary.color().withReadableWhite())
}

internal fun calendarPeriodRgb(palette: AppPalette, custom: CustomPalette): Int =
    if (palette == AppPalette.CUSTOM) custom.secondaryRgb else 0xB71C1C

internal fun calendarPredictedPeriodColor(periodColor: Color): Color = periodColor.copy(alpha = 0.28f)

internal fun calendarEntryRgb(palette: AppPalette, custom: CustomPalette): Int =
    if (palette == AppPalette.CUSTOM) custom.entryRgb else 0x1565C0

internal fun parseRgbHex(value: String): Int? {
    val normalized = value.trim().removePrefix("#")
    return normalized.takeIf { it.length == 6 && it.all { char -> char.digitToIntOrNull(16) != null } }
        ?.toIntOrNull(16)
}

@Composable
fun SeliaCyclesTheme(
    theme: AppTheme,
    palette: AppPalette,
    customPalette: CustomPalette,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    MaterialTheme(colorScheme = paletteColorScheme(palette, customPalette, dark), content = content)
}

internal fun paletteColorScheme(palette: AppPalette, custom: CustomPalette, dark: Boolean): ColorScheme = when (palette) {
    AppPalette.SELIA -> if (dark) seliaDarkColors else seliaLightColors
    AppPalette.ROSE -> if (dark) roseDarkColors else roseLightColors
    AppPalette.OCEAN -> if (dark) oceanDarkColors else oceanLightColors
    else -> derivedScheme(if (dark) seliaDarkColors else seliaLightColors, paletteRgb(palette, custom), dark)
}

private fun derivedScheme(base: ColorScheme, rgb: PaletteRgb, dark: Boolean): ColorScheme {
    val background = if (dark) Color.Black else Color.White
    val containerAmount = if (dark) 0.58f else 0.78f
    val primary = rgb.primary.color().readableOn(base.surface).readableOn(base.surfaceVariant)
    val secondary = rgb.secondary.color().readableOn(base.surface).readableOn(base.surfaceVariant)
    val tertiary = rgb.tertiary.color().readableOn(base.surface).readableOn(base.surfaceVariant)
    val primaryContainer = primary.mix(background, containerAmount)
    val secondaryContainer = secondary.mix(background, containerAmount)
    val tertiaryContainer = tertiary.mix(background, containerAmount)
    return base.copy(
        primary = primary,
        onPrimary = primary.contrastColor(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.contrastColor(),
        secondary = secondary,
        onSecondary = secondary.contrastColor(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.contrastColor(),
        tertiary = tertiary,
        onTertiary = tertiary.contrastColor(),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = tertiaryContainer.contrastColor(),
    )
}

internal fun Int.color(): Color = Color(0xFF000000L or toLong())

internal fun Color.contrastColor(): Color {
    val luminance = luminance()
    val whiteContrast = 1.05f / (luminance + 0.05f)
    val blackContrast = (luminance + 0.05f) / 0.05f
    return if (whiteContrast >= blackContrast) Color.White else Color.Black
}

private fun Color.withReadableWhite(): Color {
    var result = this
    repeat(12) {
        if (1.05f / (result.luminance() + 0.05f) >= 4.5f) return result
        result = result.mix(Color.Black, 0.14f)
    }
    return result
}

private fun Color.readableOn(background: Color): Color {
    val target = background.contrastColor()
    var result = this
    repeat(20) {
        val lighter = maxOf(result.luminance(), background.luminance())
        val darker = minOf(result.luminance(), background.luminance())
        if ((lighter + 0.05f) / (darker + 0.05f) >= 4.5f) return result
        result = result.mix(target, 0.14f)
    }
    return target
}

private fun Color.mix(other: Color, amount: Float): Color = Color(
    red = red + (other.red - red) * amount,
    green = green + (other.green - green) * amount,
    blue = blue + (other.blue - blue) * amount,
    alpha = 1f,
)
