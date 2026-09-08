package com.majkeylab.seliacycles

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LegalLinksTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun aboutLinksOpenOnlyTheExpectedPublicPages() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".qa"))
        val opened = mutableListOf<String>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_VIEW) return null
                opened += intent.dataString.orEmpty()
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                val settings = context.getString(R.string.nav_settings)
                compose.waitUntil(10_000) { compose.onAllNodesWithText(settings).fetchSemanticsNodes().isNotEmpty() }
                compose.onAllNodesWithText(settings).onLast().performClick()
                compose.onNodeWithText(context.getString(R.string.section_about)).performScrollTo().performClick()
                val page = if (context.resources.configuration.locales[0].language in listOf("cs", "sk")) "legal-cs.html" else "legal.html"
                for ((label, url) in listOf(
                    R.string.privacy_policy to "https://majkey25.github.io/SeliaCycles/",
                    R.string.terms_of_use to "https://majkey25.github.io/SeliaCycles/$page#terms",
                    R.string.refund_policy to "https://majkey25.github.io/SeliaCycles/$page#refunds",
                    R.string.cookies_policy to "https://majkey25.github.io/SeliaCycles/$page#cookies",
                    R.string.source_code to "https://github.com/Majkey25/SeliaCycles",
                )) {
                    compose.onNodeWithText(context.getString(label)).performScrollTo().performClick()
                    compose.waitForIdle()
                    assertEquals(url, opened.last())
                }
                assertEquals(5, opened.size)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }
}
