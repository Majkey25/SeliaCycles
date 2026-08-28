package com.majkeylab.seliacycles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColors = lightColorScheme(
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

private val darkColors = darkColorScheme(
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

val CycleGradientStart = Color(0xFF4E36C8)
val CycleGradientEnd = Color(0xFFB73173)

@Composable
fun SeliaCyclesTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val dark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) darkColors else lightColors, content = content)
}
