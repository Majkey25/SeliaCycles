package com.majkeylab.seliacycles

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal data class WidgetAppearance(
    val opacityPercent: Int = 100,
    val backgroundRgb: Int? = null,
    val textRgb: Int? = null,
    val accentRgb: Int? = null,
) {
    init {
        require(opacityPercent in 0..100)
        require(listOf(backgroundRgb, textRgb, accentRgb).all { it == null || it in 0..0xFFFFFF })
    }

    val backgroundAlpha: Int get() = (opacityPercent * 255f / 100).roundToInt()

    fun colors(base: ColorScheme): ColorScheme {
        val surface = backgroundRgb?.color() ?: base.surface
        fun automatic(color: Color) = if (backgroundRgb == null) color else color.readableOn(surface)
        return base.copy(
            surface = surface,
            onSurface = textRgb?.color() ?: automatic(base.onSurface),
            onSurfaceVariant = textRgb?.color() ?: automatic(base.onSurfaceVariant),
            primary = accentRgb?.color() ?: automatic(base.primary),
        )
    }
}

internal val widgetAppearanceSaver = listSaver<WidgetAppearance, Int>(
    save = { listOf(it.opacityPercent, it.backgroundRgb ?: -1, it.textRgb ?: -1, it.accentRgb ?: -1) },
    restore = { WidgetAppearance(it[0], it[1].takeIf { rgb -> rgb >= 0 }, it[2].takeIf { rgb -> rgb >= 0 }, it[3].takeIf { rgb -> rgb >= 0 }) },
)

@Composable
internal fun WidgetAppearanceSettings(value: WidgetAppearance, busy: Boolean, onChange: (WidgetAppearance) -> Unit) {
    val base = MaterialTheme.colorScheme
    val colors = value.colors(base)
    var editing by remember { mutableStateOf<Int?>(null) }
    Text(stringResource(R.string.widget_appearance), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.widget_preview), style = MaterialTheme.typography.labelLarge)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(
        Brush.horizontalGradient(listOf(Color(0xFFCEDCE9), Color(0xFF30445F))),
    )) {
        Column(Modifier.fillMaxWidth().background(colors.surface.copy(alpha = value.opacityPercent / 100f)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.nav_calendar), color = colors.primary, style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (12..18).forEach { Text(it.toString(), color = colors.onSurface) }
            }
        }
    }
    val transparencyLabel = stringResource(R.string.widget_transparency, 100 - value.opacityPercent)
    Text(transparencyLabel)
    Slider(value = (100 - value.opacityPercent).toFloat(), enabled = !busy,
        onValueChange = { onChange(value.copy(opacityPercent = 100 - it.roundToInt())) },
        valueRange = 0f..100f, steps = 19, modifier = Modifier.semantics { contentDescription = transparencyLabel })
    WidgetColorRow(R.string.widget_background_color, value.backgroundRgb, base.surface, busy,
        onEdit = { editing = R.string.widget_background_color }, onReset = { onChange(value.copy(backgroundRgb = null)) })
    WidgetColorRow(R.string.widget_text_color, value.textRgb, colors.onSurface, busy,
        onEdit = { editing = R.string.widget_text_color }, onReset = { onChange(value.copy(textRgb = null)) })
    WidgetColorRow(R.string.widget_accent_color, value.accentRgb, colors.primary, busy,
        onEdit = { editing = R.string.widget_accent_color }, onReset = { onChange(value.copy(accentRgb = null)) })
    val contrast = (maxOf(colors.onSurface.luminance(), colors.surface.luminance()) + 0.05f) /
        (minOf(colors.onSurface.luminance(), colors.surface.luminance()) + 0.05f)
    if (value.opacityPercent < 100) Text(stringResource(R.string.widget_wallpaper_hint), style = MaterialTheme.typography.bodySmall)
    else if (contrast < 4.5f) Text(stringResource(R.string.widget_contrast_hint), color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall)
    TextButton(enabled = !busy && value != WidgetAppearance(), onClick = { onChange(WidgetAppearance()) }) {
        Text(stringResource(R.string.widget_reset_appearance))
    }
    editing?.let { title ->
        val initial = when (title) {
            R.string.widget_background_color -> colors.surface
            R.string.widget_text_color -> colors.onSurface
            else -> colors.primary
        }
        ColorPickerDialog(title, initial.toArgb() and 0xFFFFFF, onDismiss = { editing = null }, onSave = { rgb ->
            onChange(when (title) {
                R.string.widget_background_color -> value.copy(backgroundRgb = rgb)
                R.string.widget_text_color -> value.copy(textRgb = rgb)
                else -> value.copy(accentRgb = rgb)
            })
            editing = null
        })
    }
}

@Composable
private fun WidgetColorRow(@StringRes label: Int, rgb: Int?, automatic: Color, busy: Boolean, onEdit: () -> Unit, onReset: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(enabled = !busy, onClick = onEdit).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(rgb?.color() ?: automatic))
            Column(Modifier.weight(1f)) {
                Text(stringResource(label))
                Text(rgb?.let { "#${it.toString(16).padStart(6, '0').uppercase()}" } ?: stringResource(R.string.widget_from_app),
                    style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.Edit, contentDescription = null)
        }
        if (rgb != null) TextButton(enabled = !busy, onClick = onReset) { Text(stringResource(R.string.widget_from_app)) }
    }
}
