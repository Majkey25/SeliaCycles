package com.majkeylab.seliacycles

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
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
            compose.onNodeWithContentDescription(text(R.string.close)).performScrollTo().performClick()
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
            compose.onNodeWithContentDescription(text(R.string.close)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.close)).performClick()
            compose.onNodeWithText(profileName).performClick()
            compose.onNodeWithText(text(R.string.profile_default_name)).performClick()
            awaitProfile(viewModel, LocalProfiles.DEFAULT_ID, UiMode.STANDARD)
            assertEquals(originalLogs, viewModel.state.value.backup.logs)
            CycleStore(context, createdId).use { assertEquals(emptyList<DayLog>(), it.load().logs) }
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
