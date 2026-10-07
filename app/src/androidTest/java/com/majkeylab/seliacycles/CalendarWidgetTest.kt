package com.majkeylab.seliacycles

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale
import androidx.core.net.toUri
import android.os.SystemClock
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule

class CalendarWidgetTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun launcherCanDiscoverResizableCalendarWidget() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val provider = AppWidgetManager.getInstance(context).installedProviders.firstOrNull {
            it.provider.packageName == context.packageName && it.provider.className.endsWith(".CalendarWidget")
        }
        assertNotNull("Calendar widget must be registered with Android", provider)
        assertNotNull(provider!!.configure)
        assertTrue(provider.resizeMode != 0)
        if (InstrumentationRegistry.getArguments().getString("pinWidget") == "true") {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    assertTrue(AppWidgetManager.getInstance(activity).requestPinAppWidget(provider.provider, null, null))
                }
            }
        }
    }

    @Test fun remoteCalendarRendersAppColorsRecordsAndPrivacyMode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val today = LocalDate.of(2026, 10, 7)
        val profile = LocalProfile(LocalProfiles.DEFAULT_ID, "Widget QA")
        val logs = (0L..4L).map { DayLog(today.plusDays(it), bleeding = true, flow = Flow.LIGHT, note = if (it == 0L) "Private note" else "") }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                listOf(AppTheme.LIGHT, AppTheme.DARK).forEach { theme ->
                    val settings = AppSettings(theme = theme, palette = AppPalette.CUSTOM,
                        customPalette = CustomPalette(primaryRgb = 0x6F4BA7, secondaryRgb = 0xBA1745, tertiaryRgb = 0x00695C))
                    val content = CycleContent(CycleBackup(logs, settings), referenceDate = today)
                    val remote = calendarWidgetViews(context, 77, profile, content, YearMonth.from(today), true)
                    // A launcher inflates platform Views, not AppCompat activity replacements.
                    val parent = FrameLayout(context)
                    val view = remote.apply(context, parent)
                    parent.addView(view)
                    activity.setContentView(parent)
                    val width = (340 * context.resources.displayMetrics.density).toInt()
                    val height = (360 * context.resources.displayMetrics.density).toInt()
                    view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    view.layout(0, 0, width, height)
                    val date = view.findViewWithText<TextView>("7")
                    assertEquals(calendarPeriodRgb(settings.palette, settings.customPalette).color().contrastColor().toArgb(), date.currentTextColor)
                    val day = date.parent as View
                    assertTrue(day.contentDescription.contains(context.getString(R.string.recorded_legend)))
                    assertFalse(day.contentDescription.contains("Private note"))
                    assertEquals(View.VISIBLE, day.findViewById<ImageView>(R.id.widget_note).visibility)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    try {
                        view.draw(Canvas(bitmap))
                        File(context.getExternalFilesDir(null), "widget-${theme.name.lowercase()}.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    } finally { bitmap.recycle() }
                    val hidden = calendarWidgetViews(context, 77, profile, content, YearMonth.from(today), false).apply(context, parent)
                    assertFalse((hidden.findViewWithText<TextView>("7").parent as View).contentDescription.contains(context.getString(R.string.recorded_legend)))
                    val czech = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("cs")) })
                    val localized = calendarWidgetViews(czech, 77, profile, content, YearMonth.from(today), false).apply(context, parent)
                    assertEquals(czech.getString(R.string.today_heading), localized.findViewById<TextView>(R.id.widget_today).text)
                    assertEquals(czech.getString(R.string.previous_month), localized.findViewById<View>(R.id.widget_previous).contentDescription)
                }
            }
        }
    }

    @Test fun widgetKeepsItsProfileAndClearsDeletedProfileInsteadOfFallingBack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val profiles = LocalProfiles(context)
        val original = profiles.selected()
        val bound = profiles.create("Bound profile")
        val other = profiles.create("Other profile")
        val prefs = CalendarWidget.preferences(context)
        try {
            CycleStore(context, bound.id).use { it.saveLog(DayLog(LocalDate.now(), bleeding = true, flow = Flow.LIGHT)) }
            CycleStore(context, other.id).use { it.saveLog(DayLog(LocalDate.now(), note = "Secret")) }
            check(prefs.edit().putString("profile_77", bound.id).putBoolean("details_77", true).commit())
            profiles.select(other.id)
            val parent = FrameLayout(context)
            val first = CalendarWidget.loadViews(context, 77).apply(context, parent)
            assertTrue(first.findViewById<TextView>(R.id.widget_brand).text.contains(bound.name))
            assertFalse(first.findViewById<TextView>(R.id.widget_brand).text.contains(other.name))
            context.deleteDatabase(profileDatabaseName(bound.id))
            profiles.remove(bound.id)
            CalendarWidget.loadViews(context, 77).reapply(context, first)
            assertEquals(context.getString(R.string.widget_choose_profile), first.findViewById<TextView>(R.id.widget_message).text)
            assertEquals(View.VISIBLE, first.findViewById<TextView>(R.id.widget_message).visibility)
            assertEquals(context.getString(R.string.app_name), first.findViewById<TextView>(R.id.widget_brand).text)
            assertEquals(0, first.findViewById<ViewGroup>(R.id.widget_weeks).childCount)
            check(prefs.edit().putString("profile_77", other.id).commit())
            CalendarWidget.loadViews(context, 77).reapply(context, first)
            assertEquals(View.VISIBLE, first.findViewById<View>(R.id.widget_navigation).visibility)
            assertEquals(View.VISIBLE, first.findViewById<View>(R.id.widget_today).visibility)
            assertEquals(View.GONE, first.findViewById<View>(R.id.widget_message).visibility)
            assertTrue(first.findViewById<ViewGroup>(R.id.widget_weeks).childCount > 0)
        } finally {
            profiles.select(original.id)
            listOf(bound, other).forEach {
                context.deleteDatabase(profileDatabaseName(it.id))
                if (profiles.profiles().any { profile -> profile.id == it.id }) profiles.remove(it.id)
            }
            prefs.edit().remove("profile_77").remove("details_77").commit()
        }
    }

    @Test fun customWidgetColorsAndBackgroundOpacitySurviveReloadAndReset() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val profiles = LocalProfiles(context)
        val profile = profiles.create("Appearance QA")
        val prefs = CalendarWidget.preferences(context)
        try {
            CycleStore(context, profile.id).use { it.saveSettings(AppSettings(theme = AppTheme.LIGHT)) }
            check(prefs.edit().putString("profile_78", profile.id)
                .putInt("opacity_78", 25).putInt("background_78", 0x102030)
                .putInt("text_78", 0xFAFAFA).putInt("accent_78", 0xFFCC00).commit())
            val parent = FrameLayout(context)
            val view = CalendarWidget.loadViews(context, 78).apply(context, parent)
            assertEquals(64, view.findViewById<ImageView>(R.id.widget_background).imageAlpha)
            assertEquals(0xFFFAFAFA.toInt(), view.findViewById<TextView>(R.id.widget_brand).currentTextColor)
            assertEquals(0xFFFFCC00.toInt(), view.findViewById<TextView>(R.id.widget_month).currentTextColor)
            check(prefs.edit().putInt("opacity_78", 0).commit())
            CalendarWidget.loadViews(context, 78).reapply(context, view)
            assertEquals(0, view.findViewById<ImageView>(R.id.widget_background).imageAlpha)
            check(prefs.edit().remove("opacity_78").remove("background_78").remove("text_78").remove("accent_78").commit())
            CalendarWidget.loadViews(context, 78).reapply(context, view)
            assertEquals(255, view.findViewById<ImageView>(R.id.widget_background).imageAlpha)
            assertEquals(paletteColorScheme(AppPalette.OCEAN, CustomPalette(), false).primary.toArgb(), view.findViewById<TextView>(R.id.widget_month).currentTextColor)
        } finally {
            context.deleteDatabase(profileDatabaseName(profile.id))
            profiles.remove(profile.id)
            prefs.edit().remove("profile_78").remove("opacity_78").remove("background_78").remove("text_78").remove("accent_78").commit()
        }
    }

    @Test fun nativeHostUpdatesNavigatesAndOpensTheBoundProfile() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".qa"))
        val manager = AppWidgetManager.getInstance(context)
        val host = AppWidgetHost(context, 3107)
        val id = host.allocateAppWidgetId()
        val profiles = LocalProfiles(context)
        val original = profiles.selected()
        val profile = profiles.create("Host QA")
        val today = LocalDate.now()
        CycleStore(context, profile.id).use { it.saveLog(DayLog(today, bleeding = true, flow = Flow.LIGHT)) }
        try {
            instrumentation.uiAutomation.adoptShellPermissionIdentity(android.Manifest.permission.BIND_APPWIDGET)
            try { assertTrue(manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, CalendarWidget::class.java))) }
            finally { instrumentation.uiAutomation.dropShellPermissionIdentity() }
            ActivityScenario.launch<CalendarWidgetConfiguration>(CalendarWidget.configureIntent(context, id)).use {
                compose.onNodeWithText(profile.name).performClick()
                compose.onNodeWithContentDescription(context.getString(R.string.widget_show_details)).performClick()
                compose.onNodeWithText(context.getString(R.string.widget_background_color)).performScrollTo().performClick()
                compose.onAllNodesWithText(context.getString(R.string.save)).onLast().performClick()
                compose.onNodeWithContentDescription(context.getString(R.string.widget_transparency, 0)).performScrollTo()
                    .performSemanticsAction(SemanticsActions.SetProgress) { it(75f) }
                compose.onNodeWithText(context.getString(R.string.widget_appearance)).performScrollTo()
                compose.waitForIdle()
                val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                try { File(context.getExternalFilesDir(null), "widget-appearance-settings.png").outputStream().use {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
                } } finally { screenshot.recycle() }
                compose.waitUntil(10_000) {
                    runCatching { compose.onNodeWithText(context.getString(R.string.save)).assertIsEnabled(); true }.getOrDefault(false)
                }
                compose.onNodeWithText(context.getString(R.string.save)).performScrollTo().performClick()
                compose.waitUntil(10_000) { CalendarWidget.preferences(context).getString("profile_$id", null) == profile.id }
                assertEquals(25, CalendarWidget.appearance(context, id).opacityPercent)
                assertNotNull(CalendarWidget.appearance(context, id).backgroundRgb)
            }
            ActivityScenario.launch<CalendarWidgetConfiguration>(CalendarWidget.configureIntent(context, id)).use {
                compose.onNodeWithText(context.getString(R.string.widget_reset_appearance)).performScrollTo().performClick()
                compose.onNodeWithContentDescription(context.getString(R.string.widget_transparency, 0)).assertExists()
                compose.onNodeWithText(context.getString(R.string.cancel)).performScrollTo().performClick()
            }
            assertEquals(25, CalendarWidget.appearance(context, id).opacityPercent)
            assertNotNull(CalendarWidget.appearance(context, id).backgroundRgb)
            // Keep the same intent identity when the PendingIntent delivers onNewIntent.
            val launch = Intent(context, MainActivity::class.java).setData("selia://widget/$id/${profile.id}/$today".toUri())
            ActivityScenario.launch<MainActivity>(launch).use { scenario ->
                lateinit var hostView: AppWidgetHostView
                lateinit var viewModel: MainViewModel
                scenario.onActivity { activity ->
                    viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                    host.startListening()
                    hostView = host.createView(context, id, manager.getAppWidgetInfo(id))
                    activity.setContentView(hostView)
                }
                fun awaitUi(check: () -> Boolean) {
                    val deadline = SystemClock.elapsedRealtime() + 10_000
                    var done = false
                    while (!done && SystemClock.elapsedRealtime() < deadline) {
                        instrumentation.runOnMainSync { done = check() }
                        if (!done) SystemClock.sleep(50)
                    }
                    assertTrue("Widget host did not receive the expected update", done)
                }
                val locale = context.resources.configuration.locales[0]
                fun title(month: YearMonth) = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
                awaitUi { hostView.findViewById<TextView>(R.id.widget_month)?.text == title(YearMonth.from(today)) }
                CalendarWidget.monthClick(context, id, 1).send()
                awaitUi { hostView.findViewById<TextView>(R.id.widget_month)?.text == title(YearMonth.from(today).plusMonths(1)) }
                CalendarWidget.monthClick(context, id, 0).send()
                awaitUi { hostView.findViewById<TextView>(R.id.widget_month)?.text == title(YearMonth.from(today)) }
                assertFalse(CalendarWidget.preferences(context).contains("month_$id"))
                CalendarWidget.openDay(context, id, profile.id, today).send()
                awaitUi { !viewModel.state.value.loading && viewModel.state.value.activeProfile.id == profile.id &&
                    viewModel.calendarDestination.value == profile.id to today }
                withContext(Dispatchers.Main) { viewModel.saveLog(DayLog(today, note = "Hidden detail")).await() }
                awaitUi {
                    val date = hostView.findViewWithText<TextView>(today.dayOfMonth.toString())
                    (date.parent as View).contentDescription.contains(context.getString(R.string.recorded_values))
                }
            }
        } finally {
            host.stopListening()
            host.deleteAppWidgetId(id)
            CalendarWidget.preferences(context).edit().remove("profile_$id").remove("month_$id").remove("details_$id")
                .remove("opacity_$id").remove("background_$id").remove("text_$id").remove("accent_$id").commit()
            profiles.select(original.id)
            context.deleteDatabase(profileDatabaseName(profile.id))
            profiles.remove(profile.id)
        }
    }
}

private inline fun <reified T : View> View.findViewWithText(text: String): T {
    val matches = ArrayList<View>()
    findViewsWithText(matches, text, View.FIND_VIEWS_WITH_TEXT)
    return matches.filterIsInstance<T>().first { it is TextView && it.text.toString() == text }
}
