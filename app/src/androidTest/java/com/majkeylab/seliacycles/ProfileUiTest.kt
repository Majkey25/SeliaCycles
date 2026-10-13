package com.majkeylab.seliacycles

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.now()
    private val profileName = "QA profile ${UUID.randomUUID().toString().take(8)}"
    private var originalIds = emptySet<String>()
    private val originalLogs = (0L..3L).flatMap { cycle -> (0L..4L).map {
        DayLog(today.minusDays(95 - cycle * 28).plusDays(it), bleeding = true, flow = Flow.UNKNOWN, note = "QA original")
    }
    }

    @Before
    fun seedQaDefaultExplicitly() {
        check(context.packageName.endsWith(".qa")) { "UI tests must never use personal app data" }
        val profiles = LocalProfiles(context)
        originalIds = profiles.profiles().mapTo(mutableSetOf(), LocalProfile::id)
        profiles.select(LocalProfiles.DEFAULT_ID)
        profiles.update(LocalProfiles.DEFAULT_ID, "", UiMode.STANDARD)
        CycleStore(context, LocalProfiles.DEFAULT_ID).use { store ->
            store.clearAll()
            store.replace(CycleBackup(
                logs = originalLogs,
                settings = AppSettings(theme = AppTheme.DARK, firstDayOfWeek = DayOfWeek.MONDAY),
            ))
        }
    }

    @After
    fun removeOnlyTheProfileCreatedByThisTest() {
        check(context.packageName.endsWith(".qa"))
        val profiles = LocalProfiles(context)
        profiles.select(LocalProfiles.DEFAULT_ID)
        profiles.profiles().filter { it.id !in originalIds && it.name == profileName }.forEach { profile ->
            check(profile.id != LocalProfiles.DEFAULT_ID)
            ReminderWorker.cancel(context, profile.id)
            context.deleteDatabase(profileDatabaseName(profile.id))
            profiles.remove(profile.id)
        }
    }

    @Test
    fun createSimpleProfileSwitchToDetailedAndKeepOriginalCalendar() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var viewModel: MainViewModel
            scenario.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
            awaitProfile(viewModel, LocalProfiles.DEFAULT_ID, UiMode.STANDARD)
            capture("code21-today.png")
            manageProfile(text(R.string.profile_default_name))
            compose.onNodeWithText(text(R.string.profile_delete)).assertDoesNotExist()
            capture("code21-profiles.png")
            compose.onNode(hasText(text(R.string.profile_create)) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.profile_name)).performTextInput(profileName)
            compose.onNodeWithText(text(R.string.ui_mode_simple)).performScrollTo().performClick()
            compose.onNode(hasText(text(R.string.profile_create)) and hasClickAction()).performScrollTo().performClick()
            compose.waitUntil(15_000) {
                viewModel.state.value.let { !it.busy && !it.loading && it.activeProfile.name == profileName }
            }
            val createdId = viewModel.state.value.activeProfile.id
            assertTrue(createdId != LocalProfiles.DEFAULT_ID)
            awaitProfile(viewModel, createdId, UiMode.SIMPLE)
            assertEquals(emptyList<DayLog>(), viewModel.state.value.backup.logs)
            CycleStore(context, LocalProfiles.DEFAULT_ID).use { assertEquals(originalLogs, it.load().logs) }
            compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
            compose.onNodeWithText(text(R.string.calendar_add_period)).performScrollTo().assertIsDisplayed()
            openTodayInformation()
            compose.onNodeWithText(text(R.string.more_details)).assertDoesNotExist()
            compose.onNodeWithText(text(R.string.wellbeing_trackers)).assertDoesNotExist()
            compose.onNodeWithContentDescription(text(R.string.close)).assertIsDisplayed().performClick()
            compose.onNodeWithText(text(R.string.close)).performClick()
            manageProfile(profileName)
            compose.onNodeWithText(text(R.string.ui_mode_detailed)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.save)).performScrollTo().performClick()
            awaitProfile(viewModel, createdId, UiMode.DETAILED)
            compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
            openTodayInformation()
            compose.onNodeWithText(text(R.string.fewer_details)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(text(R.string.wellbeing_trackers)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(text(R.string.weight_kg)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription(text(R.string.close)).assertIsDisplayed().performClick()
            compose.onNodeWithText(text(R.string.close)).performClick()
            compose.onNodeWithText(profileName).performClick()
            compose.onNodeWithText(text(R.string.profile_default_name)).performClick()
            awaitProfile(viewModel, LocalProfiles.DEFAULT_ID, UiMode.STANDARD)
            assertEquals(originalLogs, viewModel.state.value.backup.logs)
            CycleStore(context, createdId).use { assertEquals(emptyList<DayLog>(), it.load().logs) }
        }
    }

    @Test fun directProfileCreationKeepsNameIconAndSeparateCalendar() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var model: MainViewModel
            scenario.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
            awaitProfile(model, LocalProfiles.DEFAULT_ID, UiMode.STANDARD)
            compose.onNodeWithText(text(R.string.profile_default_name)).performClick()
            compose.onNodeWithText(text(R.string.profile_create)).performClick()
            compose.onNodeWithText(text(R.string.profile_name)).performTextInput(profileName)
            compose.onNodeWithText(text(R.string.profile_icon_star)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.ui_mode_simple)).performScrollTo().performClick()
            compose.onNode(hasText(text(R.string.profile_create)) and hasClickAction()).performScrollTo().performClick()
            compose.waitUntil(10_000) { !model.state.value.busy && model.state.value.activeProfile.name == profileName }
            val id = model.state.value.activeProfile.id
            awaitProfile(model, id, UiMode.SIMPLE)
            assertEquals(ProfileIcon.STAR, model.state.value.activeProfile.icon)
            scenario.recreate()
            manageProfile(profileName)
            compose.onNodeWithText(text(R.string.profile_icon_flower)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.save)).performScrollTo().performClick()
            compose.waitUntil(10_000) { !model.state.value.busy && model.state.value.activeProfile.icon == ProfileIcon.FLOWER }
            compose.waitForIdle()
            capture("profile-icons.png")
            assertEquals(emptyList<DayLog>(), model.state.value.backup.logs)
            CycleStore(context).use { assertEquals(originalLogs, it.load().logs) }
        }
    }

    @Test
    fun failedProfileCanReturnToHealthyCalendarWithoutRepairingItsDatabase() {
        val damaged = LocalProfiles(context).create(profileName)
        CycleStore(context, damaged.id).use {
            it.saveLog(DayLog(today, note = "Preserve damaged profile data"))
            it.saveSettings(AppSettings(cycleLengthOverride = 43))
            it.writableDatabase.execSQL("UPDATE settings SET theme = 'INVALID_TEST_THEME' WHERE id = 1")
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var viewModel: MainViewModel
            scenario.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
            awaitProfile(viewModel, LocalProfiles.DEFAULT_ID, UiMode.STANDARD)
            compose.onNodeWithText(text(R.string.profile_default_name)).performClick()
            compose.onNodeWithText(profileName).performClick()
            compose.waitUntil(15_000) {
                viewModel.state.value.let { !it.loading && !it.busy && it.loadFailed && it.activeProfile.id == damaged.id }
            }
            compose.onNodeWithText(text(R.string.profile_retry_load)).assertIsDisplayed()
            compose.onNode(hasText(profileName) and hasClickAction()).assertIsEnabled().performClick()
            compose.onNodeWithText(text(R.string.profile_default_name)).assertIsEnabled().performClick()
            awaitProfile(viewModel, LocalProfiles.DEFAULT_ID, UiMode.STANDARD)
            assertFalse(viewModel.state.value.loadFailed)
            assertEquals(originalLogs, viewModel.state.value.backup.logs)
            compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
            compose.onNodeWithText(text(R.string.calendar_add_period)).performScrollTo().assertIsDisplayed()
            CycleStore(context, damaged.id).use { store ->
                store.readableDatabase.rawQuery("SELECT theme FROM settings WHERE id = 1", null).use {
                    check(it.moveToFirst())
                    assertEquals("INVALID_TEST_THEME", it.getString(0))
                }
            }
        }
    }

    @Test
    fun calendarMonthFocusFilterAndScrollSurviveHistoryAndSettings() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var viewModel: MainViewModel
            scenario.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
            awaitProfile(viewModel, LocalProfiles.DEFAULT_ID, UiMode.STANDARD)
            compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
            compose.onNodeWithContentDescription(text(R.string.previous_month)).performClick()
            compose.waitForIdle()
            val month = YearMonth.from(today).minusMonths(1)
            val locale = context.resources.configuration.locales[0]
            val monthLabel = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                .replaceFirstChar { it.titlecase(locale) }
            val page = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
                hasAnyDescendant(hasText(monthLabel))
            val focused = month.atDay(15)
            compose.onNode(hasContentDescription(format(focused, FormatStyle.LONG), substring = true) and hasAnyAncestor(page))
                .performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.close)).performClick()
            compose.onNodeWithContentDescription(text(R.string.calendar_filter)).performClick()
            compose.onNodeWithText(text(R.string.note)).performClick().assertIsSelected()
            compose.onNodeWithText(text(R.string.close)).performClick()
            compose.onNode(hasText(text(R.string.month_overview)) and hasAnyAncestor(page)).performScrollTo()
            val scroll = compose.onNode(page).assertIsDisplayed().fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange].value()
            listOf(R.string.nav_history, R.string.nav_settings).forEach { destination ->
                compose.onAllNodesWithText(text(destination)).onLast().performClick()
                compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
                compose.waitForIdle()
                val restoredScroll = compose.onNode(page).assertIsDisplayed().fetchSemanticsNode()
                    .config[SemanticsProperties.VerticalScrollAxisRange].value()
                assertEquals("Calendar scroll changed after ${text(destination)}", scroll, restoredScroll, 1f)
                compose.onNode(hasContentDescription(format(focused, FormatStyle.LONG), substring = true) and
                    hasContentDescription(text(R.string.calendar_selected), substring = true) and hasAnyAncestor(page))
                    .assertExists()
                compose.onNodeWithContentDescription(text(R.string.calendar_filter)).performClick()
                compose.onNodeWithText(text(R.string.note)).assertIsSelected()
                compose.onNodeWithText(text(R.string.close)).performClick()
            }
        }
    }

    @Test
    fun longHistoryKeepsMatchingInfoAcrossRecreationAndScrollsToOldestStart() {
        val logs = (0L until 400L).map {
            DayLog(today.minusDays(5L + it * 28L), bleeding = true, flow = Flow.UNKNOWN)
        }.sortedBy(DayLog::day)
        val actualStart = logs.last().day
        CycleStore(context).use { store ->
            store.replace(CycleBackup(logs, store.load().settings))
            store.saveForecastSnapshots(listOf(ForecastSnapshot(
                YearMonth.from(actualStart), actualStart, actualStart.minusDays(1), actualStart.plusDays(1), 5, false,
            )))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var viewModel: MainViewModel
            scenario.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
            awaitProfile(viewModel, LocalProfiles.DEFAULT_ID, UiMode.STANDARD)
            assertEquals(400, viewModel.state.value.prediction.periodStarts.size)
            compose.onAllNodesWithText(text(R.string.nav_history)).onLast().performClick()
            compose.onNode(hasText(text(R.string.history_heading)) and
                hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))).assertIsDisplayed()
            val list = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
            list.performScrollToNode(hasText(text(R.string.prediction_accuracy_summary)))
            compose.onNodeWithText(text(R.string.prediction_accuracy_summary)).performClick()
            compose.onNodeWithText(text(R.string.accuracy_matching_info)).assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText(text(R.string.accuracy_matching_info)).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.close)).performClick()
            compose.onNodeWithText(text(R.string.accuracy_matching_info)).assertDoesNotExist()
            val oldest = logs.first().day
            compose.onNodeWithText(format(oldest, FormatStyle.MEDIUM)).assertDoesNotExist()
            list.performScrollToKey(oldest.toEpochDay())
            compose.onNodeWithText(format(oldest, FormatStyle.MEDIUM)).assertIsDisplayed()
        }
    }

    @Test
    fun calendarButtonBelowGridEditsFocusedDayAndKeepsTodayDistinct() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var viewModel: MainViewModel
            scenario.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
            awaitProfile(viewModel, LocalProfiles.DEFAULT_ID, UiMode.STANDARD)
            compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
            compose.onNode(
                hasContentDescription(format(today, FormatStyle.LONG), substring = true) and
                    hasContentDescription(text(R.string.today_heading), substring = true),
            ).performScrollTo().assertIsDisplayed()
            capture("code21-calendar.png")
            val focused = today.minusDays(1)
            if (YearMonth.from(focused) != YearMonth.from(today)) {
                compose.onNodeWithContentDescription(text(R.string.previous_month)).performClick()
                compose.waitForIdle()
            }
            compose.onNodeWithContentDescription(format(focused, FormatStyle.LONG), substring = true)
                .performScrollTo().performClick()
            compose.onNodeWithText(format(focused, FormatStyle.LONG)).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.close)).performClick()
            compose.onNode(
                hasContentDescription(format(focused, FormatStyle.LONG), substring = true) and
                    hasContentDescription(text(R.string.calendar_selected), substring = true),
            ).assertExists()
            val button = compose.onNodeWithText(text(R.string.calendar_add_period)).performScrollTo()
            val gridEnd = CalendarPaging.gridDays(YearMonth.from(focused), DayOfWeek.MONDAY).last()
            val lastCell = compose.onNodeWithContentDescription(format(gridEnd, FormatStyle.LONG), substring = true)
                .fetchSemanticsNode().boundsInRoot
            button.assertIsDisplayed()
            assertTrue("Final grid row must remain visible above the action", lastCell.height > 0f)
            assertTrue("Period action must sit below the month grid", button.fetchSemanticsNode().boundsInRoot.top >= lastCell.bottom)
            button.performClick()
            val editorDays = CalendarPaging.periodEditorDays(focused, DayOfWeek.MONDAY)
            compose.onNodeWithText(context.getString(
                R.string.period_editor_range,
                format(editorDays.first(), FormatStyle.MEDIUM),
                format(editorDays.last(), FormatStyle.MEDIUM),
            )).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.clear_period)).performClick()
            compose.onNodeWithContentDescription("${format(focused, FormatStyle.MEDIUM)}, ${text(R.string.period_day_not_selected)}")
                .performScrollTo().performClick()
            compose.onNodeWithContentDescription("${format(focused, FormatStyle.MEDIUM)}, ${text(R.string.period_day_selected)}")
                .assertIsDisplayed()
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.waitUntil(10_000) {
                viewModel.state.value.let { !it.busy && it.logsByDay[focused]?.bleeding == true }
            }
            assertEquals(null, viewModel.state.value.logsByDay[today])
            assertEquals(originalLogs, viewModel.state.value.backup.logs.filter { it.day != focused })
        }
    }

    private fun manageProfile(name: String) {
        compose.onNodeWithText(name).performClick()
        compose.onNodeWithText(text(R.string.profile_manage)).performClick()
        compose.onNodeWithText(text(R.string.profiles_privacy)).assertIsDisplayed()
    }

    private fun openTodayInformation() {
        compose.onNodeWithContentDescription(format(today, FormatStyle.LONG), substring = true).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.add_information)).performScrollTo().performClick()
    }

    private fun awaitProfile(viewModel: MainViewModel, id: String, mode: UiMode) {
        compose.waitUntil(15_000) {
            viewModel.state.value.let { !it.busy && !it.loading && it.activeProfile.id == id && it.activeProfile.mode == mode }
        }
        compose.waitForIdle()
    }

    private fun text(id: Int) = context.getString(id)

    private fun format(day: LocalDate, style: FormatStyle) = day.format(
        DateTimeFormatter.ofLocalizedDate(style).withLocale(context.resources.configuration.locales[0]),
    )

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            File(context.getExternalFilesDir(null), name).outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
