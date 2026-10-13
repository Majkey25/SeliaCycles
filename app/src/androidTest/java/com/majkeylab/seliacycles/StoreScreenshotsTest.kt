package com.majkeylab.seliacycles

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.junit.After
import org.junit.Rule
import org.junit.Test

class StoreScreenshotsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val originalLocales = AppCompatDelegate.getApplicationLocales()

    @After
    fun restoreAppLocale() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setApplicationLocales(originalLocales)
        }
    }

    @Test
    fun captureRealScreensWithSyntheticRecords() {
        check(context.packageName.endsWith(".qa"))
        val locale = InstrumentationRegistry.getArguments().getString("storeLocale", "en")
        check(locale in listOf("en", "cs"))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
        }
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
                settings = AppSettings(theme = AppTheme.LIGHT),
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
            val shortDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(locale))
            compose.onNodeWithContentDescription("${today.format(shortDate)}, ${text(R.string.period_day_selected)}")
                .performScrollTo().assertIsDisplayed()
            capture("03-period-editor.png")
            compose.onNodeWithText(text(R.string.cancel)).performClick()
            compose.onNodeWithText(text(R.string.discard_changes)).performClick()
            val fullDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.forLanguageTag(locale))
            compose.onNodeWithContentDescription(today.format(fullDate), substring = true).performClick()
            capture("04-day-overview.png")
            compose.onNodeWithText(text(R.string.edit_information)).performScrollTo().performClick()
            capture("06-information.png")
            compose.onNodeWithContentDescription(text(R.string.close)).assertIsDisplayed().performClick()
            compose.onNodeWithText(text(R.string.close)).performClick()
            navigate(R.string.nav_history)
            capture("05-history.png")
            navigate(R.string.nav_today)
            compose.onNodeWithText(text(R.string.phase_dashboard_body)).performScrollTo().performClick()
            capture("07-phase-guidance.png")
            compose.onNodeWithContentDescription(text(R.string.close)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.self_care_dashboard_body)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.care_for_you)).assertIsDisplayed()
            capture("09-self-care.png")
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

    private fun text(id: Int): String {
        val locale = InstrumentationRegistry.getArguments().getString("storeLocale", "en")
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(locale)) }
        return context.createConfigurationContext(configuration).getString(id)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            val locale = InstrumentationRegistry.getArguments().getString("storeLocale", "en")
            val directory = File(context.getExternalFilesDir(null), "store-code33-$locale").apply { check(isDirectory || mkdirs()) }
            File(directory, name).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }
}
