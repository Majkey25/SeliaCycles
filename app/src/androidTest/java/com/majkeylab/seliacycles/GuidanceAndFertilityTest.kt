package com.majkeylab.seliacycles

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GuidanceAndFertilityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.now()
    private lateinit var model: MainViewModel

    @Before fun resetQaOnly() {
        check(context.packageName.endsWith(".qa"))
        LocalProfiles(context).apply { select(LocalProfiles.DEFAULT_ID); update(LocalProfiles.DEFAULT_ID, "", UiMode.STANDARD) }
        CycleStore(context).use { it.clearAll() }
    }

    @Test fun differentBleedingDaysShowDifferentGuidanceAndMatchingCare() {
        val start = YearMonth.from(today).minusMonths(1).atDay(10)
        CycleStore(context).use { it.replace(CycleBackup(period(start, 6))) }
        launch().use {
            calendar()
            compose.onNodeWithContentDescription(text(R.string.previous_month)).performClick()
            listOf(0L to R.string.menstrual_early_body, 2L to R.string.menstrual_middle_body,
                5L to R.string.menstrual_later_body).forEach { (offset, body) ->
                openDay(start.plusDays(offset))
                compose.onNodeWithText(text(body)).performScrollTo().assertIsDisplayed()
                if (offset == 5L) {
                    compose.onNodeWithText(text(R.string.phase_read_more)).performScrollTo().performClick()
                    compose.onNodeWithText(text(R.string.menstrual_later_care)).performScrollTo().assertIsDisplayed()
                    capture("guidance-later.png")
                    compose.onNodeWithText(text(R.string.self_care_title)).performScrollTo().performClick()
                    compose.onAllNodesWithText(text(R.string.menstrual_later_care)).onLast().performScrollTo().assertIsDisplayed()
                } else compose.onNodeWithContentDescription(text(R.string.close)).performScrollTo().performClick()
            }
        }
    }

    @Test fun editingPeriodStartRefreshesOvulationInStateAndDayUi() {
        val oldStart = today.minusDays(18)
        val newStart = today.minusDays(16)
        CycleStore(context).use { it.replace(CycleBackup(period(today.minusDays(46), 5) + period(oldStart, 5))) }
        launch().use { activity ->
            assertEquals(today.minusDays(4), model.state.value.todayInsight.fertility?.ovulation)
            activity.onActivity { model.savePeriodDays(oldStart, (0L..4L).map(newStart::plusDays).toSet()) }
            compose.waitUntil(10_000) { !model.state.value.busy && model.state.value.todayInsight.fertility?.ovulation == today }
            assertEquals(today.plusDays(14), model.state.value.prediction.nextPeriodStart)
            activity.recreate()
            calendar()
            openDay(today)
            compose.onNodeWithText(text(R.string.selected_day_ovulation)).performScrollTo().assertIsDisplayed()
            capture("recalculated-ovulation.png")
        }
    }

    private fun launch() = ActivityScenario.launch(MainActivity::class.java).also { activity ->
        activity.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
        compose.waitUntil(10_000) { !model.state.value.loading && !model.state.value.busy }
    }
    private fun calendar() { compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick() }
    private fun openDay(day: LocalDate) {
        val label = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(context.resources.configuration.locales[0]))
        compose.onNodeWithContentDescription(label, substring = true).performScrollTo().performClick()
    }
    private fun period(start: LocalDate, length: Int) = (0L until length.toLong()).map { DayLog(start.plusDays(it), bleeding = true, flow = Flow.UNKNOWN) }
    private fun text(id: Int) = context.getString(id)
    private fun capture(name: String) {
        compose.waitForIdle()
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        ui.waitForIdle(100, 3_000)
        val bitmap = checkNotNull(ui.takeScreenshot())
        try { File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
}
