package com.majkeylab.seliacycles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppPaletteTest {
    @Test
    fun `new users start with the current light Ocean appearance`() {
        assertEquals(AppTheme.LIGHT, AppSettings().theme)
        assertEquals(AppPalette.OCEAN, AppSettings().palette)
        assertEquals(null, AppSettings().cycleLengthOverride)
    }

    @Test
    fun `Selia defaults to gold ovulation red period and teal fertility`() {
        assertEquals(
            listOf(0xF4B400, 0xC62828, 0x00897B),
            palettePreviewColors(AppPalette.SELIA).map { it.toArgb() and 0xFFFFFF },
        )
        assertEquals(CustomPalette(0xF4B400, 0xC62828, 0x00897B), CustomPalette())
    }

    @Test
    fun `calendar keeps period red and user entries blue unless customized`() {
        assertEquals(0xB71C1C, calendarPeriodRgb(AppPalette.OCEAN, CustomPalette()))
        assertEquals(0x1565C0, calendarEntryRgb(AppPalette.OCEAN, CustomPalette()))

        val custom = CustomPalette(secondaryRgb = 0xAA1122, entryRgb = 0x1234AB)
        assertEquals(0xAA1122, calendarPeriodRgb(AppPalette.CUSTOM, custom))
        assertEquals(0x1234AB, calendarEntryRgb(AppPalette.CUSTOM, custom))
    }

    @Test
    fun `predicted period keeps the period hue with low emphasis`() {
        val recorded = Color(0xFFB71C1C)
        val predicted = calendarPredictedPeriodColor(recorded)

        assertEquals(recorded.toArgb() and 0xFFFFFF, predicted.toArgb() and 0xFFFFFF)
        assertTrue(kotlin.math.abs(predicted.alpha - 0.28f) < 0.005f)
    }

    @Test
    fun `every palette has a distinct three color preview`() {
        val previews = AppPalette.entries.filterNot { it == AppPalette.CUSTOM }.map(::palettePreviewColors)

        assertEquals(6, previews.distinct().size)
        assertTrue(previews.all { it.size == 3 && it.distinct().size == 3 })
    }

    @Test
    fun `every hero gradient color keeps readable white text`() {
        val colors = AppPalette.entries.flatMap { paletteGradientColors(it, CustomPalette()) }

        assertTrue(colors.all { color -> 1.05f / (color.luminance() + 0.05f) >= 4.5f })
    }

    @Test
    fun `custom palette uses exact configured preview colors`() {
        val custom = CustomPalette(0x112233, 0x445566, 0x778899)

        assertEquals(
            listOf(0x112233, 0x445566, 0x778899),
            palettePreviewColors(AppPalette.CUSTOM, custom).map { it.toArgb() and 0xFFFFFF },
        )
    }

    @Test
    fun `hex parser accepts exact rgb and rejects incomplete input`() {
        assertEquals(0xA1B2C3, parseRgbHex("#a1b2c3"))
        assertEquals(0xA1B2C3, parseRgbHex("A1B2C3"))
        assertEquals(null, parseRgbHex("A1B2"))
        assertEquals(null, parseRgbHex("GG1122"))
    }
}
