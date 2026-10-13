package com.majkeylab.seliacycles

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
    AppPalette.MATERIAL -> PaletteRgb(0x6750A4, 0x625B71, 0x7D5260)
    AppPalette.AMOLED -> PaletteRgb(0x90CAF9, 0xFFB4AB, 0x80CBC4)
    AppPalette.GRAPHITE -> PaletteRgb(0x55575B, 0x7C5265, 0x49656B)
    AppPalette.SAND -> PaletteRgb(0x87582F, 0xA4474D, 0x587A4B)
    AppPalette.MINT -> PaletteRgb(0x00695C, 0x8B4256, 0x466742)
}

internal fun palettePreviewColors(
    palette: AppPalette,
    custom: CustomPalette = CustomPalette(),
): List<Color> = paletteRgb(palette, custom).let { listOf(it.primary.color(), it.secondary.color(), it.tertiary.color()) }

internal val LocalAppPalette = staticCompositionLocalOf { AppPalette.OCEAN }
internal val LocalAppDarkTheme = staticCompositionLocalOf { false }

internal fun paletteGradientColors(
    palette: AppPalette,
    custom: CustomPalette,
    scheme: ColorScheme,
): List<Color> {
    val colors = if (palette == AppPalette.MATERIAL) listOf(scheme.primary, scheme.secondary) else {
        paletteRgb(palette, custom).let { listOf(it.primary.color(), it.secondary.color()) }
    }
    return colors.map { it.withReadableWhite() }
}

internal fun calendarPeriodRgb(palette: AppPalette, custom: CustomPalette): Int =
    if (palette == AppPalette.CUSTOM) custom.secondaryRgb else 0xB71C1C

internal fun calendarPeriodColor(palette: AppPalette, custom: CustomPalette, surface: Color): Color =
    calendarPeriodRgb(palette, custom).color().readableOn(surface)

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
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * settings.textScalePercent / 100f),
        LocalAppPalette provides settings.paletteFor(dark),
        LocalAppDarkTheme provides dark,
    ) {
        MaterialTheme(colorScheme = appearanceColorScheme(LocalContext.current, settings, dark), content = content)
    }
}

internal fun appearanceColorScheme(context: Context, settings: AppSettings, dark: Boolean): ColorScheme {
    val palette = settings.paletteFor(dark)
    return if (palette == AppPalette.MATERIAL && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else paletteColorScheme(palette, settings.customPalette, dark)
}

internal fun paletteColorScheme(palette: AppPalette, custom: CustomPalette, dark: Boolean): ColorScheme = when (palette) {
    AppPalette.SELIA -> if (dark) seliaDarkColors else seliaLightColors
    AppPalette.ROSE -> if (dark) roseDarkColors else roseLightColors
    AppPalette.OCEAN -> if (dark) oceanDarkColors else oceanLightColors
    AppPalette.MATERIAL -> derivedScheme(if (dark) oceanDarkColors else oceanLightColors, paletteRgb(palette, custom), dark)
    AppPalette.AMOLED -> derivedScheme(neutralScheme(dark, if (dark) 0x000000 else 0xFAFAFA, if (dark) 0x202020 else 0xE4E4E4), paletteRgb(palette, custom), dark)
    AppPalette.GRAPHITE -> derivedScheme(neutralScheme(dark, if (dark) 0x151619 else 0xF8F8F8, if (dark) 0x303237 else 0xE2E3E5), paletteRgb(palette, custom), dark)
    AppPalette.SAND -> derivedScheme(neutralScheme(dark, if (dark) 0x211B15 else 0xFDF7EC, if (dark) 0x40352B else 0xEFE2D0), paletteRgb(palette, custom), dark)
    AppPalette.MINT -> derivedScheme(neutralScheme(dark, if (dark) 0x111B17 else 0xF3FAF5, if (dark) 0x2B3E34 else 0xDAEADF), paletteRgb(palette, custom), dark)
    AppPalette.CUSTOM -> customSurfaces(derivedScheme(if (dark) seliaDarkColors else seliaLightColors, paletteRgb(palette, custom), dark), custom)
    else -> derivedScheme(if (dark) seliaDarkColors else seliaLightColors, paletteRgb(palette, custom), dark)
}

private fun neutralScheme(dark: Boolean, background: Int, variant: Int): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val surface = background.color()
    val text = surface.contrastColor()
    return base.copy(background = surface, onBackground = text, surface = surface, onSurface = text,
        surfaceVariant = variant.color(), onSurfaceVariant = text, surfaceContainerLowest = surface,
        surfaceContainerLow = surface.mix(text, 0.03f), surfaceContainer = surface.mix(text, 0.05f),
        surfaceContainerHigh = surface.mix(text, 0.08f), surfaceContainerHighest = variant.color(),
        surfaceBright = surface.mix(text, 0.06f), surfaceDim = surface)
}

private fun customSurfaces(base: ColorScheme, custom: CustomPalette): ColorScheme {
    if (custom.backgroundRgb == null && custom.surfaceRgb == null && custom.textRgb == null) return base
    val background = custom.backgroundRgb?.color() ?: base.background
    val text = (custom.textRgb?.color() ?: base.onSurface).readableOn(background)
    fun readableSurface(requested: Color): Color {
        var result = requested
        repeat(20) {
            if (contrast(text, result) >= 4.5f) return result
            result = result.mix(background, 0.2f)
        }
        return background
    }
    val surface = readableSurface(custom.surfaceRgb?.color() ?: background)
    val variant = readableSurface(surface.mix(text, 0.06f))
    fun accent(requested: Color): Color = requested.readableOn(background).readableOn(surface).readableOn(variant)
        .takeIf { color -> listOf(background, surface, variant).all { contrast(color, it) >= 4.5f } } ?: text
    return base.copy(background = background, onBackground = text, surface = surface, onSurface = text,
        surfaceVariant = variant, onSurfaceVariant = text,
        primary = accent(base.primary), onPrimary = accent(base.primary).contrastColor(),
        secondary = accent(base.secondary), onSecondary = accent(base.secondary).contrastColor(),
        tertiary = accent(base.tertiary), onTertiary = accent(base.tertiary).contrastColor(),
        primaryContainer = readableSurface(base.primaryContainer), onPrimaryContainer = text,
        secondaryContainer = readableSurface(base.secondaryContainer), onSecondaryContainer = text,
        tertiaryContainer = readableSurface(base.tertiaryContainer), onTertiaryContainer = text,
        error = accent(base.error), onError = accent(base.error).contrastColor(),
        errorContainer = readableSurface(base.errorContainer), onErrorContainer = text,
        outline = text, outlineVariant = text.copy(alpha = 0.35f),
        surfaceContainerLowest = background, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = variant, surfaceContainerHighest = variant, surfaceBright = variant, surfaceDim = background)
}

private fun contrast(first: Color, second: Color): Float =
    (maxOf(first.luminance(), second.luminance()) + 0.05f) / (minOf(first.luminance(), second.luminance()) + 0.05f)

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

internal fun Color.readableOn(background: Color): Color {
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
