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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContraceptionUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int) = context.getString(id)

    @Test fun recordTakenReopenDayAndEditReminder() {
        check(context.packageName.endsWith(".qa"))
        LocalProfiles(context).apply { select(LocalProfiles.DEFAULT_ID); update(LocalProfiles.DEFAULT_ID, "", UiMode.SIMPLE) }
        val today = LocalDate.now()
        CycleStore(context).use { it.replace(CycleBackup(settings = AppSettings(
            profile = UserProfile(lifeSituation = LifeSituation.HORMONAL_CONTRACEPTION)))) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { activity ->
                lateinit var model: MainViewModel
                activity.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
                compose.waitUntil(10_000) { !model.state.value.loading && !model.state.value.busy }
                compose.onAllNodesWithText(text(R.string.nav_calendar)).onLast().performClick()
                val date = today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(context.resources.configuration.locales[0]))
                compose.onNodeWithContentDescription(date, substring = true).performScrollTo().performClick()
                compose.onNodeWithText(text(R.string.add_information)).performScrollTo().performClick()
                compose.onNodeWithText(text(R.string.medication_taken)).performScrollTo().performClick()
                compose.onNodeWithText(text(R.string.save)).assertIsDisplayed().performClick()
                compose.waitUntil(10_000) { !model.state.value.busy && model.state.value.logsByDay[today]?.contraception == ContraceptionStatus.TAKEN }
                compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.save)).fetchSemanticsNodes().isEmpty() }
                compose.onNodeWithContentDescription(date, substring = true).performScrollTo().performClick()
                compose.onNodeWithText(context.getString(R.string.contraception_summary, text(R.string.medication_taken)))
                    .performScrollTo().assertIsDisplayed()
                activity.recreate()
                compose.onNodeWithText(context.getString(R.string.contraception_summary, text(R.string.medication_taken)))
                    .performScrollTo().assertIsDisplayed()
                compose.onNodeWithContentDescription(text(R.string.close)).performScrollTo().performClick()
                compose.onAllNodesWithText(text(R.string.nav_settings)).onLast().performClick()
                compose.onNodeWithText(text(R.string.section_reminders)).performScrollTo().performClick()
                compose.onNodeWithText(text(R.string.contraception_reminder)).performScrollTo().performClick()
                compose.waitUntil(10_000) { !model.state.value.busy && model.state.value.backup.settings.contraceptionReminderEnabled }
                compose.onNodeWithText(context.getString(R.string.contraception_time, "20:00")).performScrollTo().assertIsDisplayed()
                val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                try { File(context.getExternalFilesDir(null), "contraception-settings.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                finally { bitmap.recycle() }
                compose.onNodeWithText(text(R.string.contraception_reminder)).performScrollTo().performClick()
                compose.waitUntil(10_000) { !model.state.value.busy && !model.state.value.backup.settings.contraceptionReminderEnabled }
                assertEquals(ContraceptionStatus.TAKEN, CycleStore(context).use { it.load().logs.single().contraception })
            }
        } finally { ContraceptionReminder.cancel(context, LocalProfiles.DEFAULT_ID) }
    }
}
