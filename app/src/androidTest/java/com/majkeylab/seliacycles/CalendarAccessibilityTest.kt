package com.majkeylab.seliacycles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CalendarAccessibilityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.now()

    @Before fun seedOnlyQa() {
        check(context.packageName.endsWith(".qa"))
        LocalProfiles(context).apply {
            select(LocalProfiles.DEFAULT_ID)
            update(LocalProfiles.DEFAULT_ID, "", UiMode.STANDARD)
        }
        CycleStore(context).use {
            it.clearAll()
            it.replace(CycleBackup(listOf(DayLog(today, note = "QA marker")), settings = AppSettings(
                theme = AppTheme.DARK, palette = AppPalette.CUSTOM,
                customPalette = CustomPalette(entryRgb = 0x191210),
                firstDayOfWeek = if (today.withDayOfMonth(1).dayOfWeek == DayOfWeek.MONDAY) DayOfWeek.SUNDAY else DayOfWeek.MONDAY,
            )))
        }
    }

    @Test fun datesDoNotExposeDuplicateBareNumerals() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openCalendar()
            compose.onAllNodesWithText(today.dayOfMonth.toString(), useUnmergedTree = true).assertCountEquals(0)
            compose.onNodeWithContentDescription(date(today), substring = true).assertExists()
        }
    }

    @Test fun adjacentMonthDateTextHasReadableContrast() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openCalendar()
            val adjacent = today.withDayOfMonth(1).minusDays(1)
            val bounds = compose.onNodeWithContentDescription(date(adjacent), substring = true).fetchSemanticsNode().boundsInRoot
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            val background = pixels[bounds.left.toInt() + 2, bounds.top.toInt() + 2]
            var strongest = 1f
            for (y in bounds.top.toInt() until bounds.bottom.toInt()) {
                for (x in bounds.left.toInt() until bounds.right.toInt()) strongest = maxOf(strongest, contrast(pixels[x, y], background))
            }
            assertTrue("Adjacent date contrast: $strongest", strongest >= 4.5f)
        }
    }

    @Test fun customMarkerCannotDisappearAgainstMatchingSurface() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openCalendar()
            val bounds = compose.onNodeWithContentDescription(date(today), substring = true).fetchSemanticsNode().boundsInRoot
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            val density = context.resources.displayMetrics.density
            val y = (bounds.center.y + 24.5f * density).roundToInt()
            val marker = pixels[bounds.center.x.roundToInt(), y]
            val background = pixels[(bounds.center.x - 14f * density).roundToInt(), y]
            assertTrue("Marker contrast: ${contrast(marker, background)}", contrast(marker, background) >= 3f)
        }
    }

    private fun openCalendar() {
        val label = context.getString(R.string.nav_calendar)
        compose.waitUntil(10_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(label).onLast().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(context.getString(R.string.calendar_add_period)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun date(day: LocalDate) = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(context.resources.configuration.locales[0]))

    private fun contrast(first: Color, second: Color): Float =
        (maxOf(first.luminance(), second.luminance()) + 0.05f) / (minOf(first.luminance(), second.luminance()) + 0.05f)
}
