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
import org.junit.Rule
import org.junit.Test

class StoreScreenshotsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun captureRealScreensWithSyntheticRecords() {
        check(context.packageName.endsWith(".qa"))
        val today = LocalDate.now()
        LocalProfiles(context).apply {
            select(LocalProfiles.DEFAULT_ID)
            update(LocalProfiles.DEFAULT_ID, "", UiMode.STANDARD)
        }
        CycleStore(context).use { store ->
            store.clearAll()
            store.replace(CycleBackup(
                logs = (0L..3L).flatMap { cycle -> (0L..4L).map { offset ->
                    DayLog(today.minusDays(95 - cycle * 28).plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
                } } + DayLog(today, mood = Mood.GOOD, energy = WellbeingLevel.MEDIUM, note = "Rested well."),
                settings = AppSettings(theme = AppTheme.DARK, profile = UserProfile(age = 20, heightCm = 172, weightKg = 50.0)),
            ))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var viewModel: MainViewModel
            scenario.onActivity { viewModel = ViewModelProvider(it)[MainViewModel::class.java] }
            compose.waitUntil(15_000) { !viewModel.state.value.loading && !viewModel.state.value.busy }
            capture("01-home.png")
            navigate(R.string.nav_calendar)
            compose.onNodeWithText(text(R.string.calendar_add_period)).assertIsDisplayed()
            capture("02-calendar.png")
            compose.onNodeWithText(text(R.string.calendar_add_period)).performClick()
            val shortDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(context.resources.configuration.locales[0])
            compose.onNodeWithContentDescription("${today.format(shortDate)}, ${text(R.string.period_day_not_selected)}")
                .performScrollTo().performClick()
            capture("03-period-editor.png")
            compose.onNodeWithText(text(R.string.cancel)).performClick()
            capture("04-day-overview.png")
            compose.onNodeWithText(text(R.string.edit_information)).performScrollTo().performClick()
            capture("06-information.png")
            compose.onNodeWithContentDescription(text(R.string.close)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.close)).performClick()
            navigate(R.string.nav_history)
            capture("05-history.png")
            navigate(R.string.nav_today)
            compose.onNodeWithText(text(R.string.phase_dashboard_body)).performScrollTo().performClick()
            capture("07-phase-guidance.png")
            compose.onNodeWithContentDescription(text(R.string.close)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.profile_default_name)).performClick()
            compose.onNodeWithText(text(R.string.profile_manage)).performClick()
            capture("08-settings.png")
        }
    }

    private fun navigate(label: Int) {
        compose.onAllNodesWithText(text(label)).onLast().performClick()
        compose.waitForIdle()
    }

    private fun text(id: Int) = context.getString(id)

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            val directory = File(context.getExternalFilesDir(null), "store-code21").apply { check(isDirectory || mkdirs()) }
            File(directory, name).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }
}
