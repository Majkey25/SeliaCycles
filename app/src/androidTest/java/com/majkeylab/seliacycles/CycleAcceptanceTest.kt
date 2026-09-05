package com.majkeylab.seliacycles

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CycleAcceptanceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.now()

    @Before
    fun seedIsolatedApp() {
        check(context.packageName.endsWith(".qa")) { "Device tests must never use the personal app database" }
        LocalProfiles(context).select(LocalProfiles.DEFAULT_ID)
        LocalProfiles(context).update(LocalProfiles.DEFAULT_ID, "", UiMode.STANDARD)
        CycleStore(context).use { store ->
            store.clearAll()
            store.replace(CycleBackup(
                logs = (0L..3L).flatMap { cycle ->
                    (0L..4L).map { day -> DayLog(today.minusDays(90 - cycle * 28).plusDays(day), bleeding = true, flow = Flow.UNKNOWN) }
                },
                settings = AppSettings(cycleLengthOverride = 31, periodLengthOverride = 6, theme = AppTheme.DARK),
            ))
        }
    }

    @Test
    fun rapidSettingsKeepTheLatestChoice() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitNavigation()
            lateinit var viewModel: MainViewModel
            scenario.onActivity { activity ->
                viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                for (length in 26..45) {
                    val settings = viewModel.state.value.backup.settings
                    viewModel.saveSettings(settings.copy(cycleLengthOverride = length, periodLengthOverride = 6))
                }
            }
            compose.waitUntil(10_000) { !viewModel.state.value.busy && viewModel.state.value.backup.settings.cycleLengthOverride == 45 }
            assertEquals(45, CycleStore(context).use { it.load().settings.cycleLengthOverride })
            assertEquals(6, CycleStore(context).use { it.load().settings.periodLengthOverride })
        }
    }

    @Test
    fun measureStateWithMaximumSupportedHistory() {
        val backup = CycleBackup(List(CycleBackup.MAX_LOGS) { index ->
            val bleeding = index % 28 < 5
            DayLog(today.minusDays((CycleBackup.MAX_LOGS - index).toLong()), bleeding = bleeding,
                flow = if (bleeding) Flow.UNKNOWN else Flow.NONE, mood = Mood.GOOD)
        })
        val buildTimes = mutableListOf<Long>()
        val copyTimes = mutableListOf<Long>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            repeat(5) { AppState(content = CycleContent(backup = backup, referenceDate = today)) }
            val state = AppState(content = CycleContent(backup = backup, referenceDate = today))
            repeat(21) {
                buildTimes += kotlin.system.measureNanoTime { assertFalse(AppState(content = CycleContent(backup = backup, referenceDate = today)).periodEstimates.isEmpty()) }
                copyTimes += kotlin.system.measureNanoTime { assertFalse(state.copy(busy = true).periodEstimates.isEmpty()) }
            }
        }
        File(context.getExternalFilesDir(null), "state-timing.txt").writeText(
            "construct median/max ms: ${buildTimes.sorted()[10] / 1_000_000.0}/${buildTimes.max() / 1_000_000.0}\n" +
                "status copy median/max ms: ${copyTimes.sorted()[10] / 1_000_000.0}/${copyTimes.max() / 1_000_000.0}\n",
        )
    }

    @Test
    fun localExportMergesOverExistingSnapshotsAndKeepsSharingPrivate() {
        CycleStore(context).use { store ->
            val date = today.minusMonths(1).withDayOfMonth(10)
            val snapshot = ForecastSnapshot(YearMonth.from(date), date, date.minusDays(2), date.plusDays(2), 5, false)
            store.saveForecastSnapshots(listOf(snapshot))
            val before = store.load()
            val exported = ByteArrayOutputStream()
            MyCalendarExporter(context).write(SeliaTransfer(before, listOf(snapshot)), exported)
            val parsed = MyCalendarImporter(context).inspect(ByteArrayInputStream(exported.toByteArray()))
            val transfer = requireNotNull(parsed.seliaTransfer)
            store.mergeTransfer(transfer.copy(backup = transfer.backup.copy(settings = transfer.backup.settings.copy(partnerViewEnabled = true))))
            assertEquals(before.logs, store.load().logs)
            assertEquals(listOf(snapshot), store.loadForecastSnapshots())
            assertFalse(store.load().settings.partnerViewEnabled)
        }
    }

    @Test
    fun independentAutomaticSettingsPersistWithoutChangingTheOtherLength() {
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitNavigation()
            compose.onAllNodesWithText(text(R.string.nav_settings)).onLast().performClick()
            compose.onNodeWithText(text(R.string.settings_cycle)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.prediction_auto_cycle)).performScrollTo().performClick()
            compose.waitUntil(5_000) { CycleStore(context).use { store -> store.load().settings.cycleLengthOverride == null } }
            assertEquals(6, CycleStore(context).use { store -> store.load().settings.periodLengthOverride })
            compose.onNodeWithText(text(R.string.prediction_auto_period)).performScrollTo().performClick()
            compose.waitUntil(5_000) { CycleStore(context).use { store -> store.load().settings.periodLengthOverride == null } }
        }
    }

    @Test
    fun informationDraftSurvivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            awaitNavigation()
            compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
            compose.onNodeWithContentDescription(today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .withLocale(context.resources.configuration.locales[0])), substring = true).performClick()
            compose.onNodeWithText(text(R.string.add_information)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.note)).performScrollTo().performTextInput("QA unfinished note")
            activity.recreate()
            compose.onNodeWithText("QA unfinished note").performScrollTo().assertIsDisplayed()
            assertEquals(null, CycleStore(context).use { store -> store.load().logs.firstOrNull { it.day == today } })
        }
    }

    @Test
    fun periodStartLengthChangeAndRemovalUpdateFutureButKeepOriginalForecast() {
        val snapshotDay = YearMonth.from(today).atDay(12)
        val snapshot = ForecastSnapshot(YearMonth.from(today), snapshotDay, snapshotDay.minusDays(2), snapshotDay.plusDays(2), 5, false)
        CycleStore(context).use { store ->
            store.replace(CycleBackup(
                logs = (0L..3L).flatMap { cycle -> (0L..4L).map { day ->
                    DayLog(today.minusDays(114 - cycle * 28).plusDays(day), bleeding = true, flow = Flow.UNKNOWN)
                } } + DayLog(today, note = "QA keep"),
                settings = AppSettings(cycleLengthOverride = 31, periodLengthOverride = 6),
            ))
            store.saveForecastSnapshots(listOf(snapshot))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitNavigation()
            compose.onNodeWithText(text(R.string.period_start_action)).performClick()
            compose.waitUntil(5_000) { CycleStore(context).use { store -> store.load().settings.activePeriodStart == today } }
            compose.onAllNodesWithText(text(R.string.nav_settings)).onLast().performClick()
            compose.onNodeWithText(text(R.string.settings_cycle)).performScrollTo().performClick()
            for (length in 32..35) {
                compose.onNodeWithContentDescription(context.getString(R.string.increase_value, text(R.string.default_cycle_length)))
                    .performScrollTo().performClick()
                compose.waitUntil(5_000) { CycleStore(context).use { store -> store.load().settings.cycleLengthOverride == length } }
            }
            CycleStore(context).use { store ->
                val future = CycleInsights.calendarPeriodEstimates(store.load(), emptyMap(), today).first()
                assertEquals(today.plusDays(35), future.start)
                assertEquals(6L, java.time.temporal.ChronoUnit.DAYS.between(future.start, future.endExclusive))
                assertEquals(snapshot, store.loadForecastSnapshots().first { it.month == snapshot.month })
            }
            compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
            compose.onNodeWithText(text(R.string.calendar_add_period)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.clear_period)).performClick()
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.waitUntil(5_000) { CycleStore(context).use { store -> store.load().logs.none { it.day == today && it.bleeding } } }
            CycleStore(context).use { store ->
                assertEquals("QA keep", store.load().logs.first { it.day == today }.note)
                assertEquals(snapshot, store.loadForecastSnapshots().first { it.month == snapshot.month })
            }
        }
    }

    @Test
    fun monthOverviewShowsDataAndLinksIntoTheDay() {
        CycleStore(context).use { it.saveLog(DayLog(today, bleeding = true, flow = Flow.UNKNOWN)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitNavigation()
            compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
            compose.onNodeWithText(text(R.string.month_overview)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.month_bleeding_days)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(text(R.string.month_timeline)).performScrollTo().assertIsDisplayed()
            File(context.getExternalFilesDir(null), "month-overview.png").outputStream().use { output ->
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            compose.onNodeWithText(text(R.string.month_timeline_hint)).performScrollTo().assertIsDisplayed()
            File(context.getExternalFilesDir(null), "month-timeline.png").outputStream().use { output ->
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            compose.onNodeWithText(text(R.string.month_observations_empty)).performScrollTo().assertIsDisplayed()
            val locale = context.resources.configuration.locales[0]
            val shortFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            compose.onNodeWithText(today.format(shortFormat)).performScrollTo().performClick()
            compose.onNodeWithText(today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.edit_period)).assertIsDisplayed()
        }
    }

    private fun text(id: Int) = context.getString(id)

    private fun awaitNavigation() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.nav_calendar)).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
}
