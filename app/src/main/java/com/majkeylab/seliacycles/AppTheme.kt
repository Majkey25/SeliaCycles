package com.majkeylab.seliacycles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColors = lightColorScheme(
    primary = Color(0xFF713B55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E7),
    onPrimaryContainer = Color(0xFF3A1028),
    secondary = Color(0xFF8C4A5E),
    secondaryContainer = Color(0xFFFFD9E0),
    background = Color(0xFFFFF8FA),
    surface = Color(0xFFFFF8FA),
    surfaceVariant = Color(0xFFF2E3E7),
    outline = Color(0xFF857278),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFFFAFCC),
    onPrimary = Color(0xFF541D3B),
    primaryContainer = Color(0xFF6E3452),
    onPrimaryContainer = Color(0xFFFFD8E7),
    secondary = Color(0xFFFFB1C2),
    secondaryContainer = Color(0xFF713344),
    background = Color(0xFF181114),
    surface = Color(0xFF181114),
    surfaceVariant = Color(0xFF3A2D31),
    outline = Color(0xFFA18B91),
)

@Composable
fun SeliaCyclesTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val dark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) darkColors else lightColors, content = content)
}
