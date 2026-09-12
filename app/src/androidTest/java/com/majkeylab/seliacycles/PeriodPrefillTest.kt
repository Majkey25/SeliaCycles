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
import java.time.LocalDate
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PeriodPrefillTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.now()
    private val history = (0L..5L).map { DayLog(today.minusDays(35).plusDays(it), bleeding = true, flow = Flow.UNKNOWN) }
    private lateinit var viewModel: MainViewModel

    @Before fun seedQaOnly() {
        check(context.packageName.endsWith(".qa"))
        LocalProfiles(context).apply { select(LocalProfiles.DEFAULT_ID); update(LocalProfiles.DEFAULT_ID, "", UiMode.STANDARD) }
        CycleStore(context).use { it.clearAll(); it.replace(CycleBackup(history)) }
    }

    @Test fun historicalEntryPrefillsLearnedLengthAndCanBeShortened() = checkPrefill(null, 6)
    @Test fun historicalEntryUsesManualLengthBeforeHistory() = checkPrefill(3, 3)

    private fun checkPrefill(override: Int?, length: Int) {
        CycleStore(context).use { it.saveSettings(AppSettings(periodLengthOverride = override)) }
        val start = today.minusDays(8)
        launch().use { activity ->
            openEditor(start)
            (0L until length.toLong()).forEach { offset -> selected(start.plusDays(offset)).assertIsDisplayed() }
            capture("period-prefill-$length.png")
            selected(start.plusDays(length - 1L)).performClick()
            activity.recreate()
            selected(start).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.waitUntil(10_000) { !viewModel.state.value.busy && viewModel.state.value.logsByDay[start]?.bleeding == true }
            val saved = CycleStore(context).use { it.load() }
            assertEquals((0L until length - 1L).map(start::plusDays), saved.logs.filter { it.day >= start && it.bleeding }.map(DayLog::day))
            assertEquals(history, saved.logs.filter { it.day < start })
        }
    }

    @Test fun todayEntryShowsContinuationWithoutSavingFutureBleeding() {
        launch().use {
            openEditor(today)
            selected(today).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.waitUntil(10_000) { !viewModel.state.value.busy && viewModel.state.value.backup.settings.activePeriodStart == today }
            val state = viewModel.state.value
            val span = state.periodEstimates.single { it.start == today }
            assertEquals(today.plusDays(6), span.endExclusive)
            assertTrue(state.backup.logs.none { it.day > today && it.bleeding })
            assertEquals(6, state.prediction.averagePeriodLength)
            compose.waitForIdle()
            capture("period-continuation.png")
            viewModel.endPeriod(today, today)
            compose.waitUntil(10_000) { !viewModel.state.value.busy && viewModel.state.value.backup.settings.activePeriodStart == null }
            assertTrue(viewModel.state.value.periodEstimates.none { it.start == today })
        }
    }

    private fun launch() = ActivityScenario.launch(MainActivity::class.java).also { scenario ->
        scenario.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
        compose.waitUntil(10_000) { !viewModel.state.value.loading && !viewModel.state.value.busy }
    }

    private fun openEditor(day: LocalDate) {
        compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
        if (YearMonth.from(day) != YearMonth.from(today)) compose.onNodeWithContentDescription(text(R.string.previous_month)).performClick()
        compose.onNodeWithContentDescription(format(day, FormatStyle.LONG), substring = true).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.close)).performClick()
        compose.onNodeWithText(text(R.string.calendar_add_period)).performScrollTo().performClick()
    }

    private fun selected(day: LocalDate) = compose.onNodeWithContentDescription("${format(day, FormatStyle.MEDIUM)}, ${text(R.string.period_day_selected)}").performScrollTo()
    private fun capture(name: String) {
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            File(context.getExternalFilesDir(null), name).outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
    }
    private fun format(day: LocalDate, style: FormatStyle) = day.format(DateTimeFormatter.ofLocalizedDate(style).withLocale(context.resources.configuration.locales[0]))
    private fun text(id: Int) = context.getString(id)
}
