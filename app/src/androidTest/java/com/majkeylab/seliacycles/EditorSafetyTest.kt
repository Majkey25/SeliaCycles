package com.majkeylab.seliacycles

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditorSafetyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val today = LocalDate.now()
    private val original = DayLog(today, bleeding = true, flow = Flow.MEDIUM,
        note = "Saved QA note", mood = Mood.GOOD, weightKg = 54.0, intimacy = Intimacy.PROTECTED,
        importedDetails = "QA imported detail")
    private val otherDay = DayLog(today.minusDays(1), note = "Keep other day")

    @Before fun seedOnlyQa() {
        check(context.packageName.endsWith(".qa"))
        LocalProfiles(context).apply {
            select(LocalProfiles.DEFAULT_ID)
            update(LocalProfiles.DEFAULT_ID, "", UiMode.SIMPLE)
        }
        CycleStore(context).use { it.clearAll(); it.replace(CycleBackup(listOf(otherDay, original))) }
    }

    @Test fun deletionNeedsConfirmationAndPreservesPeriod() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            openInformation()
            compose.onNodeWithText(text(R.string.note)).performScrollTo().performTextReplacement("Unsaved QA note")
            compose.onNodeWithText(text(R.string.delete_information)).performClick()
            compose.waitForIdle()
            assertEquals(original, savedToday())
            compose.onNodeWithText(text(R.string.delete_information_title)).assertIsDisplayed()
            activity.recreate()
            compose.onNodeWithText(text(R.string.delete_information_title)).assertIsDisplayed()
            compose.onAllNodesWithText(text(R.string.cancel)).onLast().performClick()
            compose.onNodeWithText("Unsaved QA note").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(text(R.string.delete_information)).performClick()
            compose.onNodeWithText(text(R.string.confirm_delete)).performClick()
            compose.waitUntil(5_000) { savedToday()?.hasCalendarMarker == false }
            assertEquals(DayLog(today, bleeding = true, flow = Flow.MEDIUM), savedToday())
            assertEquals(otherDay, CycleStore(context).use { it.load().logs.first { log -> log.day == otherDay.day } })
        }
    }

    @Test fun closingChangedInformationKeepsDraftUntilDiscarded() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openInformation()
            compose.onNodeWithText(text(R.string.note)).performScrollTo().performTextReplacement("Unsaved QA note")
            compose.onNodeWithContentDescription(text(R.string.close)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.unsaved_changes_title)).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.keep_editing)).performClick()
            compose.onNodeWithText("Unsaved QA note").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(text(R.string.cancel)).performClick()
            compose.onNodeWithText(text(R.string.discard_changes)).performClick()
            assertEquals(original, savedToday())
            compose.onNodeWithText(text(R.string.edit_information)).performScrollTo().performClick()
            compose.onNodeWithContentDescription(text(R.string.close)).performClick()
            compose.onNodeWithText(text(R.string.unsaved_changes_title)).assertDoesNotExist()
        }
    }

    @Test fun periodSelectionRequiresDiscardButRevertedChangesDoNot() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            openCalendar()
            compose.onNodeWithText(text(R.string.calendar_add_period)).performScrollTo().performClick()
            toggleYesterday(false)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithText(text(R.string.unsaved_changes_title)).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.keep_editing)).performClick()
            // Native dialog dismissal finishes outside the Compose test clock.
            compose.waitUntil(5_000) { compose.onNodeWithText(text(R.string.cancel)).isDisplayed() }
            compose.onNodeWithText(text(R.string.cancel)).assertIsDisplayed()
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithText(text(R.string.unsaved_changes_title)).assertIsDisplayed()
            activity.recreate()
            compose.onNodeWithText(text(R.string.keep_editing)).performClick()
            toggleYesterday(true)
            compose.onNodeWithText(text(R.string.cancel)).performClick()
            compose.onNodeWithText(text(R.string.unsaved_changes_title)).assertDoesNotExist()
            assertEquals(original, savedToday())
            assertEquals(otherDay, CycleStore(context).use { it.load().logs.first { log -> log.day == otherDay.day } })
        }
    }

    @Test fun invalidMeasurementCannotSaveButCanBeDiscarded() {
        LocalProfiles(context).update(LocalProfiles.DEFAULT_ID, "", UiMode.DETAILED)
        ActivityScenario.launch(MainActivity::class.java).use {
            openInformation()
            compose.onNodeWithText(text(R.string.weight_kg)).performScrollTo().performTextReplacement("999")
            compose.onNodeWithText(text(R.string.save)).assertIsNotEnabled()
            compose.onNodeWithText(text(R.string.cancel)).performClick()
            compose.onNodeWithText(text(R.string.unsaved_changes_title)).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.discard_changes)).performClick()
            assertEquals(original, savedToday())
        }
    }

    private fun openInformation() {
        openCalendar()
        val date = today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(context.resources.configuration.locales[0]))
        compose.onNodeWithContentDescription(date, substring = true).performClick()
        compose.onNodeWithText(text(R.string.edit_information)).performScrollTo().performClick()
    }

    private fun openCalendar() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.nav_calendar)).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.calendar_add_period)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun toggleYesterday(selected: Boolean) {
        val date = today.minusDays(1).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(context.resources.configuration.locales[0]))
        val label = text(if (selected) R.string.period_day_selected else R.string.period_day_not_selected)
        compose.onNodeWithContentDescription("$date, $label").performScrollTo().performClick()
    }

    private fun savedToday() = CycleStore(context).use { it.load().logs.firstOrNull { log -> log.day == today } }
    private fun text(id: Int) = context.getString(id)
}
