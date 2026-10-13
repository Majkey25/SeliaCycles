package com.majkeylab.seliacycles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class AppPaletteTest {
    @Test
    fun `accent text stays readable for presets and extreme custom colors`() {
        for (dark in listOf(false, true)) {
            for (palette in AppPalette.entries) {
                for (custom in listOf(CustomPalette(), CustomPalette(0xFFFFFF, 0x000000, 0xFFFF00))) {
                    val scheme = paletteColorScheme(palette, custom, dark)
                    for (accent in listOf(scheme.primary, scheme.secondary, scheme.tertiary)) {
                        for (background in listOf(scheme.surface, scheme.surfaceVariant)) {
                            val contrast = (maxOf(accent.luminance(), background.luminance()) + 0.05f) /
                                (minOf(accent.luminance(), background.luminance()) + 0.05f)
                            assertTrue(contrast >= 4.5f, "$palette dark=$dark contrast=$contrast")
                        }
                    }
                }
            }
        }
    }

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

        assertEquals(AppPalette.entries.size - 1, previews.distinct().size)
        assertTrue(previews.all { it.size == 3 && it.distinct().size == 3 })
    }

    @Test
    fun `every hero gradient color keeps readable white text`() {
        val colors = AppPalette.entries.flatMap {
            paletteGradientColors(it, CustomPalette(), paletteColorScheme(it, CustomPalette(), false))
        }

        assertTrue(colors.all { color -> 1.05f / (color.luminance() + 0.05f) >= 4.5f })
    }

    @Test
    fun `Material hero follows resolved wallpaper colors`() {
        val base = paletteColorScheme(AppPalette.MATERIAL, CustomPalette(), false)
        for (scheme in listOf(
            base.copy(primary = Color(0xFF005500), secondary = Color(0xFF003366)),
            base.copy(primary = Color(0xFF660033), secondary = Color(0xFF663300)),
        )) {
            assertEquals(listOf(scheme.primary, scheme.secondary),
                paletteGradientColors(AppPalette.MATERIAL, CustomPalette(), scheme))
        }
    }

    @Test
    fun `matching period and surface remain visible without changing stored RGB`() {
        for (dark in listOf(false, true)) for (rgb in listOf(0x000000, 0xFFFFFF, 0xC62828)) {
            val custom = CustomPalette(secondaryRgb = rgb, backgroundRgb = rgb, surfaceRgb = rgb)
            val surface = paletteColorScheme(AppPalette.CUSTOM, custom, dark).surface
            val period = calendarPeriodColor(AppPalette.CUSTOM, custom, surface)
            val predicted = calendarPredictedPeriodColor(period).compositeOver(surface)
            val recordedContrast = (maxOf(period.luminance(), surface.luminance()) + .05f) /
                (minOf(period.luminance(), surface.luminance()) + .05f)
            val predictedContrast = (maxOf(predicted.luminance(), surface.luminance()) + .05f) /
                (minOf(predicted.luminance(), surface.luminance()) + .05f)
            assertTrue(recordedContrast >= 4.5f, "rgb=$rgb dark=$dark contrast=$recordedContrast")
            assertTrue(predictedContrast > 1.1f, "rgb=$rgb dark=$dark contrast=$predictedContrast")
            assertEquals(rgb, calendarPeriodRgb(AppPalette.CUSTOM, custom))
        }
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

    @Test
    fun `light and dark palette selection stay independent with legacy fallback`() {
        val settings = AppSettings(palette = AppPalette.SAND, darkPalette = AppPalette.AMOLED)
        assertEquals(AppPalette.SAND, settings.paletteFor(false))
        assertEquals(AppPalette.AMOLED, settings.paletteFor(true))
        assertEquals(AppPalette.SAND, settings.copy(darkPalette = null).paletteFor(true))
        assertEquals(Color.Black, paletteColorScheme(AppPalette.AMOLED, CustomPalette(), true).background)
    }

    @Test
    fun `custom surfaces and text retain readable contrast at extremes`() {
        for (dark in listOf(false, true)) for (background in listOf(0x000000, 0xFFFFFF, 0x787878)) {
            for (surface in listOf(0x000000, 0xFFFFFF, 0x787878)) for (text in listOf(0x000000, 0xFFFFFF, 0x787878)) {
                val custom = CustomPalette(backgroundRgb = background, surfaceRgb = surface, textRgb = text)
                val scheme = paletteColorScheme(AppPalette.CUSTOM, custom, dark)
                assertEquals(background, scheme.background.toArgb() and 0xFFFFFF)
                for ((foreground, fill) in listOf(scheme.onBackground to scheme.background,
                    scheme.onSurface to scheme.surface, scheme.onSurfaceVariant to scheme.surfaceVariant,
                    scheme.primary to scheme.surface, scheme.primary to scheme.surfaceContainerHighest,
                    scheme.onSurface to scheme.primaryContainer, scheme.onSurface to scheme.secondaryContainer,
                    scheme.onSurface to scheme.tertiaryContainer, scheme.error to scheme.surface,
                    scheme.onErrorContainer to scheme.errorContainer)) {
                    val contrast = (maxOf(foreground.luminance(), fill.luminance()) + .05f) /
                        (minOf(foreground.luminance(), fill.luminance()) + .05f)
                    assertTrue(contrast >= 4.5f, "bg=$background surface=$surface text=$text dark=$dark contrast=$contrast")
                }
            }
        }
    }

    @Test
    fun `appearance input rejects invalid RGB and unsupported text scales`() {
        assertFailsWith<IllegalArgumentException> { CustomPalette(backgroundRgb = -1) }
        assertFailsWith<IllegalArgumentException> { CustomPalette(textRgb = 0x1000000) }
        assertFailsWith<IllegalArgumentException> { AppSettings(textScalePercent = 84) }
        assertFailsWith<IllegalArgumentException> { AppSettings(textScalePercent = 141) }
    }
}
