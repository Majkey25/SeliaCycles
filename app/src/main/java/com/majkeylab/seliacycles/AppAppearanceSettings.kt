package com.majkeylab.seliacycles

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AppAppearanceSettings(settings: AppSettings, onSave: (AppSettings) -> Unit) {
    Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
    ThemeModeSelector(settings.theme) { onSave(settings.copy(theme = it)) }
    Text(stringResource(R.string.color_palette), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.appearance_pairs_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    val context = LocalContext.current
    (AppPalette.entries.filterNot { it == AppPalette.CUSTOM } + AppPalette.CUSTOM).chunked(2).forEach { palettes ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            palettes.forEach { palette ->
                val shape = RoundedCornerShape(18.dp)
                Column(Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    .clip(shape).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { onSave(settings.copy(palette = palette, darkPalette = null)) },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(paletteLabel(palette)), fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(false, true).forEach { dark ->
                            val scheme = appearanceColorScheme(context, settings.copy(palette = palette, darkPalette = null), dark)
                            ThemeSwatch(scheme, calendarPeriodColor(palette, settings.customPalette, scheme.background),
                                dark, settings.paletteFor(dark) == palette, palette,
                                Modifier.weight(1f)) {
                                onSave(if (dark) settings.copy(darkPalette = palette) else
                                    settings.copy(palette = palette, darkPalette = settings.paletteFor(true)))
                            }
                        }
                    }
                }
            }
            if (palettes.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Text(stringResource(if (Build.VERSION.SDK_INT >= 31) R.string.appearance_material_dynamic else
        R.string.appearance_material_fallback), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    val scheme = MaterialTheme.colorScheme
    val palette = LocalAppPalette.current
    val dark = LocalAppDarkTheme.current
    OutlinedButton(onClick = {
        val updated = settings.copy(customPalette = CustomPalette(
            primaryRgb = scheme.primary.toArgb() and 0xFFFFFF,
            secondaryRgb = calendarPeriodRgb(palette, settings.customPalette),
            tertiaryRgb = scheme.tertiary.toArgb() and 0xFFFFFF,
            entryRgb = calendarEntryRgb(palette, settings.customPalette),
            backgroundRgb = scheme.background.toArgb() and 0xFFFFFF,
            surfaceRgb = scheme.surface.toArgb() and 0xFFFFFF,
            textRgb = scheme.onSurface.toArgb() and 0xFFFFFF,
        ))
        onSave(if (dark) updated.copy(darkPalette = AppPalette.CUSTOM) else
            updated.copy(palette = AppPalette.CUSTOM, darkPalette = settings.paletteFor(true)))
    }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Edit, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.appearance_customize))
    }
    var textSize by remember(settings.textScalePercent) { mutableFloatStateOf(settings.textScalePercent.toFloat()) }
    val textSizeLabel = stringResource(R.string.appearance_text_size, textSize.toInt())
    Text(textSizeLabel, style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.appearance_text_sample), fontSize = (16f * textSize / settings.textScalePercent).sp)
    Slider(value = textSize, onValueChange = { textSize = it }, valueRange = 85f..140f, steps = 10,
        onValueChangeFinished = { onSave(settings.copy(textScalePercent = textSize.toInt())) },
        modifier = Modifier.semantics { contentDescription = textSizeLabel })
    TextButton(onClick = { onSave(settings.copy(theme = AppTheme.LIGHT, palette = AppPalette.OCEAN,
        darkPalette = null, textScalePercent = 100, customPalette = CustomPalette())) }) {
        Icon(Icons.Outlined.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.appearance_reset))
    }
}

@Composable
private fun ThemeSwatch(scheme: ColorScheme, period: Color, dark: Boolean, isSelected: Boolean, palette: AppPalette,
    modifier: Modifier, onClick: () -> Unit) {
    val mode = stringResource(if (dark) R.string.theme_dark else R.string.theme_light)
    val description = stringResource(paletteLabel(palette)) + " · " + mode
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.clip(shape).border(if (isSelected) 2.dp else 1.dp,
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
        .clickable(role = Role.Button, onClick = onClick).semantics {
            contentDescription = description
            selected = isSelected
        }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(8.dp)).background(scheme.background)) {
            drawRoundRect(scheme.primary, Offset(size.width * .12f, 8.dp.toPx()),
                Size(size.width * .60f, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
            val cell = size.width / 8f
            for (row in 0..2) for (column in 0..6) {
                drawCircle(if (row == 1 && column in 1..3) period else scheme.onSurface.copy(alpha = .25f),
                    cell * .27f, Offset(cell * (column + 1), 23.dp.toPx() + row * 11.dp.toPx()))
            }
        }
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Icon(if (isSelected) Icons.Outlined.Check else if (dark) Icons.Outlined.Bedtime else Icons.Outlined.WbSunny,
                contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(mode, style = MaterialTheme.typography.labelSmall)
    }
}
