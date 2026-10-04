package com.majkeylab.seliacycles

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CalendarCleanupUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun previewRequiresSelectionAndOnlyPassesApprovedIds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val first = CalendarCleanupEvent(1, 1, "Selia · Recorded period", 1_700_000_000_000L, 1_700_086_400_000L,
            null, null, null, 1, null, null, true)
        val second = first.copy(id = 2, title = "Selia · Estimated period")
        val preview = CalendarCleanupPreview(LocalProfiles.DEFAULT_ID, DeviceCalendar(1, "QA calendar", "Local QA", 0), listOf(first, second))
        var deleted = emptySet<Long>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent { MaterialTheme { CalendarCleanupDialog(preview, false, {}, { deleted = it }) } }
            }
            compose.onNodeWithText(context.getString(R.string.calendar_cleanup_delete_selected, 0)).assertIsNotEnabled()
            compose.onNodeWithText(first.title!!).performClick()
            compose.waitForIdle()
            val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            try { File(context.getExternalFilesDir(null), "legacy-cleanup-preview.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            finally { bitmap.recycle() }
            compose.onNodeWithText(context.getString(R.string.calendar_cleanup_delete_selected, 1)).performClick()
            assertEquals(setOf(1L), deleted)
            compose.onNodeWithText(context.getString(R.string.calendar_cleanup_select_all)).performClick()
            compose.onNodeWithText(context.getString(R.string.calendar_cleanup_select_none)).performClick()
            compose.onNodeWithText(context.getString(R.string.calendar_cleanup_delete_selected, 0)).assertIsNotEnabled()
        }
    }
}
