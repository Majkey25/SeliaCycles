package com.majkeylab.seliacycles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val seliaLightColors = lightColorScheme(
    primary = Color(0xFF5840D6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E0FF),
    onPrimaryContainer = Color(0xFF20105E),
    secondary = Color(0xFFA32F68),
    secondaryContainer = Color(0xFFFFD8E7),
    tertiary = Color(0xFF006A71),
    tertiaryContainer = Color(0xFF9CF1FA),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    surfaceVariant = Color(0xFFE9E4EF),
    outline = Color(0xFF79747E),
)

private val seliaDarkColors = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF291882),
    primaryContainer = Color(0xFF4030A4),
    onPrimaryContainer = Color(0xFFE7E0FF),
    secondary = Color(0xFFFFAFD0),
    secondaryContainer = Color(0xFF7D2450),
    tertiary = Color(0xFF80D5DD),
    tertiaryContainer = Color(0xFF004F55),
    background = Color(0xFF14121A),
    surface = Color(0xFF14121A),
    surfaceVariant = Color(0xFF332F3A),
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

internal fun palettePreviewColors(palette: AppPalette): List<Color> = when (palette) {
    AppPalette.SELIA -> seliaLightColors
    AppPalette.ROSE -> roseLightColors
    AppPalette.OCEAN -> oceanLightColors
}.let { listOf(it.primary, it.secondary, it.tertiary) }

internal fun paletteGradientColors(palette: AppPalette): List<Color> = when (palette) {
    AppPalette.SELIA -> listOf(Color(0xFF4E36C8), Color(0xFFB73173))
    AppPalette.ROSE -> listOf(Color(0xFF8F2957), Color(0xFF704132))
    AppPalette.OCEAN -> listOf(Color(0xFF005C99), Color(0xFF006B65))
}

@Composable
fun SeliaCyclesTheme(theme: AppTheme, palette: AppPalette, content: @Composable () -> Unit) {
    val dark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val colors = when (palette) {
        AppPalette.SELIA -> seliaLightColors to seliaDarkColors
        AppPalette.ROSE -> roseLightColors to roseDarkColors
        AppPalette.OCEAN -> oceanLightColors to oceanDarkColors
    }
    MaterialTheme(colorScheme = if (dark) colors.second else colors.first, content = content)
}
