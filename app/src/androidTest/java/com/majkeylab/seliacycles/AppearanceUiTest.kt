package com.majkeylab.seliacycles

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppearanceUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val log = DayLog(LocalDate.now(), bleeding = true, flow = Flow.UNKNOWN, note = "QA appearance preserve")

    @Before fun seedOnlyQaData() {
        check(context.packageName.endsWith(".qa"))
        LocalProfiles(context).select(LocalProfiles.DEFAULT_ID)
        CycleStore(context, LocalProfiles.DEFAULT_ID).use {
            it.replace(CycleBackup(listOf(log), AppSettings(cycleLengthOverride = 31)))
        }
    }

    @Test fun pairedThemesPersistAcrossActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openAppearance()
            val light = context.getString(R.string.theme_light)
            val dark = context.getString(R.string.theme_dark)
            compose.onNodeWithContentDescription(context.getString(R.string.palette_sand) + " · " + light)
                .performScrollTo().performClick()
            awaitSettings { it.palette == AppPalette.SAND && it.darkPalette == AppPalette.OCEAN }
            compose.onNodeWithContentDescription(context.getString(R.string.palette_amoled) + " · " + dark)
                .performScrollTo().performClick()
            awaitSettings { it.darkPalette == AppPalette.AMOLED }
            compose.onAllNodesWithText(dark).onFirst().performScrollTo().performClick()
            awaitSettings { it.theme == AppTheme.DARK }
            compose.onNodeWithText(context.getString(R.string.color_palette)).performScrollTo()
            screenshot("appearance-dark")
            scenario.recreate()
            compose.waitForIdle()
            assertEquals(AppPalette.SAND, savedSettings().palette)
            assertEquals(AppPalette.AMOLED, savedSettings().darkPalette)
            compose.onNodeWithText(context.getString(R.string.palette_mint)).performScrollTo().performClick()
            awaitSettings { it.palette == AppPalette.MINT && it.darkPalette == null }
            CycleStore(context, LocalProfiles.DEFAULT_ID).use { assertEquals(listOf(log), it.load().logs) }
        }
    }

    @Test fun customPickerHexAndLargeTextResetWithoutChangingCycleData() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openAppearance()
            compose.onNodeWithContentDescription(context.getString(R.string.appearance_text_size, 100)).performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(130f) }
            awaitSettings { it.textScalePercent == 130 }
            compose.onNodeWithText(context.getString(R.string.appearance_customize)).performScrollTo().performClick()
            awaitSettings { it.palette == AppPalette.CUSTOM && it.customPalette.backgroundRgb != null }
            compose.onNodeWithText(context.getString(R.string.appearance_background)).performScrollTo().performClick()
            compose.onNodeWithText(context.getString(R.string.save)).performClick()
            compose.onNodeWithText(context.getString(R.string.exact_colors)).performScrollTo().performClick()
            compose.onNode(hasText(context.getString(R.string.appearance_background)) and hasSetTextAction())
                .performScrollTo().performTextReplacement("AAAAAA")
            awaitSettings { it.customPalette.backgroundRgb == 0xAAAAAA }
            scenario.recreate()
            compose.waitForIdle()
            assertEquals(130, savedSettings().textScalePercent)
            assertEquals(0xAAAAAA, savedSettings().customPalette.backgroundRgb)
            compose.onNodeWithText(context.getString(R.string.appearance_reset)).performScrollTo().performClick()
            awaitSettings { it.palette == AppPalette.OCEAN && it.darkPalette == null && it.textScalePercent == 100 }
            assertEquals(31, savedSettings().cycleLengthOverride)
            assertEquals(CustomPalette(), savedSettings().customPalette)
            CycleStore(context, LocalProfiles.DEFAULT_ID).use { assertEquals(listOf(log), it.load().logs) }
            compose.onNodeWithText(context.getString(R.string.color_palette)).performScrollTo().assertIsDisplayed()
            screenshot("appearance-light")
        }
    }

    @Test fun customizingCurrentThemePreservesTheInactivePreset() {
        for (dark in listOf(false, true)) {
            CycleStore(context, LocalProfiles.DEFAULT_ID).use {
                it.saveSettings(AppSettings(theme = if (dark) AppTheme.DARK else AppTheme.LIGHT,
                    palette = AppPalette.SAND, darkPalette = AppPalette.AMOLED))
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                openAppearance()
                compose.onNodeWithText(context.getString(R.string.appearance_customize)).performScrollTo().performClick()
                awaitSettings { it.paletteFor(dark) == AppPalette.CUSTOM }
                assertEquals(if (dark) AppPalette.SAND else AppPalette.AMOLED, savedSettings().paletteFor(!dark))
                scenario.recreate()
                compose.waitForIdle()
                assertEquals(AppPalette.CUSTOM, savedSettings().paletteFor(dark))
                assertEquals(if (dark) AppPalette.SAND else AppPalette.AMOLED, savedSettings().paletteFor(!dark))
            }
        }
    }

    @Test fun inactiveCustomPickerUsesItsOwnDefaultBackground() {
        for (dark in listOf(false, true)) {
            val expected = if (dark) 0xFFF9F7 else 0x191210
            CycleStore(context, LocalProfiles.DEFAULT_ID).use {
                it.saveSettings(AppSettings(theme = if (dark) AppTheme.DARK else AppTheme.LIGHT,
                    palette = if (dark) AppPalette.CUSTOM else AppPalette.OCEAN,
                    darkPalette = if (dark) AppPalette.OCEAN else AppPalette.CUSTOM))
            }
            ActivityScenario.launch(MainActivity::class.java).use {
                openAppearance()
                compose.onAllNodesWithText("#${expected.toString(16).uppercase()}").onFirst()
                    .performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(context.getString(R.string.appearance_background)).performScrollTo().performClick()
                compose.onNodeWithText(context.getString(R.string.save)).performClick()
                awaitSettings { it.customPalette.backgroundRgb == expected }
            }
        }
    }

    private fun openAppearance() {
        val settings = context.getString(R.string.nav_settings)
        compose.waitUntil(10_000) { compose.onAllNodesWithText(settings).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(settings).onLast().performClick()
        compose.onNodeWithText(context.getString(R.string.settings_appearance)).performScrollTo().performClick()
    }

    private fun savedSettings() = CycleStore(context, LocalProfiles.DEFAULT_ID).use { it.load().settings }
    private fun awaitSettings(predicate: (AppSettings) -> Boolean) = compose.waitUntil(10_000) { predicate(savedSettings()) }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
