package com.majkeylab.seliacycles

import android.view.KeyEvent
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SaveFailureTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.now()
    private val original = DayLog(today, bleeding = true, flow = Flow.MEDIUM, note = "Saved QA record")
    private lateinit var viewModel: MainViewModel

    @Before fun seedOnlyQa() {
        check(context.packageName.endsWith(".qa"))
        LocalProfiles(context).apply {
            select(LocalProfiles.DEFAULT_ID)
            update(LocalProfiles.DEFAULT_ID, "", UiMode.STANDARD)
        }
        repairDatabase()
        CycleStore(context).use { it.clearAll(); it.replace(CycleBackup(listOf(original))) }
    }

    @After fun repairDatabase() {
        check(context.packageName.endsWith(".qa"))
        CycleStore(context).use {
            it.writableDatabase.execSQL("DROP TRIGGER IF EXISTS qa_reject_write")
            it.writableDatabase.execSQL("UPDATE settings SET theme = 'LIGHT' WHERE id = 1")
        }
    }

    @Test fun failedDaySaveKeepsDraftThroughRecreationAndRetry() {
        launch().use { activity ->
            openInformation()
            compose.onNodeWithText(text(R.string.note)).performScrollTo().performTextReplacement("Unsaved QA draft")
            rejectWrites()
            compose.onNodeWithText(text(R.string.save)).performClick()
            awaitFailure()
            assertEquals(original, savedToday())
            compose.onNodeWithText("Unsaved QA draft").performScrollTo().assertIsDisplayed()
            val screenshot = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            try {
                File(context.getExternalFilesDir(null), "save-error.png").outputStream().use {
                    check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally {
                screenshot.recycle()
            }
            activity.recreate()
            compose.onNodeWithText("Unsaved QA draft").performScrollTo().assertIsDisplayed()
            repairDatabase()
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.waitUntil(10_000) { savedToday()?.note == "Unsaved QA draft" && !viewModel.state.value.busy }
            compose.onNodeWithText(text(R.string.note)).assertDoesNotExist()
        }
    }

    @Test fun failedPeriodSaveKeepsSelectedDaysUntilRetrySucceeds() {
        launch().use {
            openCalendar()
            compose.onNodeWithText(text(R.string.calendar_add_period)).performScrollTo().performClick()
            val yesterday = today.minusDays(1)
            val label = date(yesterday, FormatStyle.MEDIUM)
            compose.onNodeWithContentDescription("$label, ${text(R.string.period_day_not_selected)}").performClick()
            rejectWrites()
            compose.onNodeWithText(text(R.string.save)).performClick()
            awaitFailure()
            assertEquals(listOf(original), CycleStore(context).use { it.load().logs })
            compose.onNodeWithContentDescription("$label, ${text(R.string.period_day_selected)}").assertIsDisplayed()
            repairDatabase()
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.waitUntil(10_000) { CycleStore(context).use { it.load().logs.any { log -> log.day == yesterday && log.bleeding } } && !viewModel.state.value.busy }
            compose.onNodeWithText(text(R.string.edit_period)).assertDoesNotExist()
        }
    }

    @Test fun editorInputsCannotChangeDuringPendingWrite() {
        launch().use {
            openInformation()
            compose.onNodeWithText(text(R.string.note)).performScrollTo().performTextReplacement("Pending QA draft")
            CycleStore(context).use { gate ->
                gate.writableDatabase.beginTransaction()
                try {
                    compose.onNodeWithText(text(R.string.save)).performClick()
                    compose.waitUntil(5_000) { viewModel.state.value.busy }
                    compose.onNodeWithText(text(R.string.note)).assertIsNotEnabled()
                    compose.onNodeWithText(text(R.string.cancel)).assertIsNotEnabled()
                    compose.onNodeWithText(text(R.string.save)).assertIsNotEnabled()
                    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                    compose.onNodeWithText(text(R.string.note)).assertIsDisplayed()
                    compose.onNodeWithText(text(R.string.unsaved_changes_title)).assertDoesNotExist()
                } finally {
                    gate.writableDatabase.endTransaction()
                }
            }
            compose.waitUntil(10_000) { savedToday()?.note == "Pending QA draft" && !viewModel.state.value.busy }
            compose.onNodeWithText(text(R.string.note)).assertDoesNotExist()
        }
    }

    @Test fun committedWriteRemainsSavedWhenRefreshFails() {
        launch().use {
            openInformation()
            compose.onNodeWithText(text(R.string.note)).performScrollTo().performTextReplacement("Committed QA draft")
            CycleStore(context).use { it.writableDatabase.execSQL("UPDATE settings SET theme = 'INVALID_QA_THEME' WHERE id = 1") }
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.waitUntil(10_000) { viewModel.state.value.loadFailed && !viewModel.state.value.busy }
            compose.waitForIdle()
            compose.onNodeWithText(text(R.string.note)).assertDoesNotExist()
            CycleStore(context).use { store ->
                store.readableDatabase.rawQuery("SELECT note FROM day_logs WHERE day = ?", arrayOf(today.toEpochDay().toString())).use { cursor ->
                    check(cursor.moveToFirst())
                    assertEquals("Committed QA draft", cursor.getString(0))
                }
            }
            repairDatabase()
            compose.onNodeWithText(text(R.string.profile_retry_load)).performClick()
            compose.waitUntil(10_000) { !viewModel.state.value.loadFailed && !viewModel.state.value.busy }
            assertEquals("Committed QA draft", savedToday()?.note)
        }
    }

    @Test fun failedReadAfterWriteFailureOffersRetryWithoutLosingDraft() {
        launch().use {
            openInformation()
            compose.onNodeWithText(text(R.string.note)).performScrollTo().performTextReplacement("QA recovery draft")
            rejectWrites()
            CycleStore(context).use { it.writableDatabase.execSQL("UPDATE settings SET theme = 'INVALID_QA_THEME' WHERE id = 1") }
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.waitUntil(10_000) { viewModel.state.value.loadFailed && !viewModel.state.value.busy }
            compose.onNodeWithText("QA recovery draft").performScrollTo().assertIsDisplayed()
            repairDatabase()
            compose.onAllNodesWithText(text(R.string.profile_retry_load)).onLast().performClick()
            compose.waitUntil(10_000) { !viewModel.state.value.loadFailed && !viewModel.state.value.busy }
            compose.onNodeWithText("QA recovery draft").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.waitUntil(10_000) { savedToday()?.note == "QA recovery draft" && !viewModel.state.value.busy }
        }
    }

    @Test fun pendingSuccessfulSaveReconnectsAfterRecreation() = checkPendingRecreation(reject = false)

    @Test fun pendingFailedSaveReconnectsAfterRecreation() = checkPendingRecreation(reject = true)

    private fun checkPendingRecreation(reject: Boolean) {
        launch().use { activity ->
            openInformation()
            compose.onNodeWithText(text(R.string.note)).performScrollTo().performTextReplacement("QA recreated draft")
            if (reject) rejectWrites()
            CycleStore(context).use { gate ->
                gate.writableDatabase.beginTransaction()
                try {
                    compose.onNodeWithText(text(R.string.save)).performClick()
                    compose.waitUntil(5_000) { viewModel.state.value.busy }
                    activity.recreate()
                    compose.onNodeWithText("QA recreated draft").assertIsNotEnabled()
                } finally {
                    gate.writableDatabase.endTransaction()
                }
            }
            compose.waitUntil(10_000) { !viewModel.state.value.busy }
            compose.waitForIdle()
            if (reject) {
                compose.onNodeWithText(text(R.string.save_failed_keep_draft)).assertIsDisplayed()
                compose.onNodeWithText("QA recreated draft").performScrollTo().assertIsDisplayed()
                assertEquals(original, savedToday())
                repairDatabase()
                compose.onNodeWithText(text(R.string.save)).performClick()
                compose.waitUntil(10_000) { savedToday()?.note == "QA recreated draft" && !viewModel.state.value.busy }
            } else assertEquals("QA recreated draft", savedToday()?.note)
            compose.onNodeWithText(text(R.string.note)).assertDoesNotExist()
        }
    }

    private fun launch() = ActivityScenario.launch(MainActivity::class.java).also { scenario ->
        scenario.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
        compose.waitUntil(10_000) { !viewModel.state.value.loading && !viewModel.state.value.busy }
    }

    private fun openCalendar() {
        compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.calendar_add_period)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun openInformation() {
        openCalendar()
        compose.onNodeWithContentDescription(date(today, FormatStyle.LONG), substring = true).performClick()
        compose.onNodeWithText(text(R.string.edit_information)).performScrollTo().performClick()
    }

    private fun rejectWrites() = CycleStore(context).use {
        it.writableDatabase.execSQL("CREATE TRIGGER qa_reject_write BEFORE INSERT ON day_logs BEGIN SELECT RAISE(ABORT, 'QA write failure'); END")
    }

    private fun awaitFailure() = compose.waitUntil(10_000) {
        !viewModel.state.value.busy && viewModel.state.value.message == R.string.operation_failed
    }

    private fun savedToday() = CycleStore(context).use { it.load().logs.firstOrNull { log -> log.day == today } }
    private fun date(day: LocalDate, style: FormatStyle) = day.format(DateTimeFormatter.ofLocalizedDate(style)
        .withLocale(context.resources.configuration.locales[0]))
    private fun text(id: Int) = context.getString(id)
}
