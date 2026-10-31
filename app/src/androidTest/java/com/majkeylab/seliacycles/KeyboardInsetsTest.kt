package com.majkeylab.seliacycles

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class KeyboardInsetsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun seedOnlyQa() {
        check(context.packageName.endsWith(".qa"))
        LocalProfiles(context).apply {
            select(LocalProfiles.DEFAULT_ID)
            update(LocalProfiles.DEFAULT_ID, "", UiMode.DETAILED)
        }
        CycleStore(context).use { it.replace(CycleBackup(emptyList())) }
    }

    @Test fun appearanceFieldsRemainReachableAboveTheKeyboard() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openTab(R.string.nav_settings)
            compose.onNodeWithText(text(R.string.settings_appearance)).performClick()
            compose.onNodeWithText(text(R.string.appearance_customize)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.exact_colors)).performScrollTo().performClick()
            val field = compose.onNode(hasText(text(R.string.appearance_background)) and hasSetTextAction())
            field.performScrollTo().performClick().performTextReplacement("ABCDEF")
            awaitKeyboard(scenario)
            field.performScrollTo().assertIsDisplayed()
            var visibleBottom = 0
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                val ime = requireNotNull(ViewCompat.getRootWindowInsets(decor)).getInsets(WindowInsetsCompat.Type.ime()).bottom
                visibleBottom = decor.height - ime
            }
            val bottom = field.fetchSemanticsNode().boundsInWindow.bottom
            capture("appearance-keyboard.png")
            assertTrue("Field must fit above IME: $bottom > $visibleBottom", bottom <= visibleBottom + 1)
        }
    }

    @Test fun dailyNoteCanBeSavedWithoutDismissingTheKeyboard() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openTab(R.string.nav_calendar)
            val date = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .withLocale(context.resources.configuration.locales[0]))
            compose.onNodeWithContentDescription(date, substring = true).performClick()
            compose.onNodeWithText(text(R.string.add_information)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.note)).performScrollTo().performClick().performTextReplacement("Keyboard regression")
            awaitKeyboard(scenario)
            capture("daily-note-keyboard.png")
            compose.onNode(hasText(text(R.string.save)) and hasClickAction()).assertIsDisplayed().performClick()
            compose.waitUntil(10_000) {
                CycleStore(context).use { it.load().logs.any { log -> log.note == "Keyboard regression" } }
            }
            assertEquals("Keyboard regression", CycleStore(context).use { it.load().logs.single().note })
        }
    }

    private fun openTab(label: Int) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(label)).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(text(label)).onLast().performClick()
    }

    private fun awaitKeyboard(scenario: ActivityScenario<MainActivity>) {
        try {
            compose.waitUntil(10_000) {
                var visible = false
                scenario.onActivity { visible = ViewCompat.getRootWindowInsets(it.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true }
                visible
            }
        } catch (error: Exception) {
            capture("keyboard-failure.png")
            throw error
        }
    }

    private fun text(id: Int) = context.getString(id)

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            val folder = File(context.getExternalFilesDir(null), "keyboard-insets")
            check(folder.isDirectory || folder.mkdirs())
            File(folder, name).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
    }
}
