package com.majkeylab.seliacycles

import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppPaletteTest {
    @Test
    fun `Selia palette remains the default`() {
        assertEquals(AppPalette.SELIA, AppSettings().palette)
    }

    @Test
    fun `every palette has a distinct three color preview`() {
        val previews = AppPalette.entries.map(::palettePreviewColors)

        assertEquals(3, previews.distinct().size)
        assertTrue(previews.all { it.size == 3 && it.distinct().size == 3 })
    }

    @Test
    fun `every hero gradient color keeps readable white text`() {
        val colors = AppPalette.entries.flatMap(::paletteGradientColors)

        assertTrue(colors.all { color -> 1.05f / (color.luminance() + 0.05f) >= 4.5f })
    }
}
