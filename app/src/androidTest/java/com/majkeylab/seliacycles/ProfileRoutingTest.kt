package com.majkeylab.seliacycles

import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileRoutingTest {
    @Test fun launcherIgnoresInternalExtrasWhilePrivateActivityRoutes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val profiles = LocalProfiles(context)
        val original = profiles.selected()
        val other = profiles.create("Routing regression")
        try {
            val extras = Intent(context, MainActivity::class.java)
                .putExtra(ReminderWorker.PROFILE_ID_EXTRA, other.id)
            ActivityScenario.launch<MainActivity>(extras).use { scenario ->
                lateinit var model: MainViewModel
                scenario.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
                val ready = withTimeout(15_000) { model.state.first { !it.loading && !it.busy } }
                assertEquals(original.id, ready.activeProfile.id)
                scenario.onActivity { it.startActivity(Intent(extras).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                assertEquals(original.id, model.state.value.activeProfile.id)
            }
            val activity = context.packageManager.getActivityInfo(ComponentName(context, ProfileActivity::class.java), 0)
            assertFalse(activity.exported)
            ActivityScenario.launch<ProfileActivity>(Intent(extras).setClass(context, ProfileActivity::class.java)).use { scenario ->
                lateinit var model: MainViewModel
                scenario.onActivity { model = ViewModelProvider(it)[MainViewModel::class.java] }
                val ready = withTimeout(15_000) { model.state.first { !it.loading && !it.busy && it.activeProfile.id == other.id } }
                assertEquals(other.id, ready.activeProfile.id)
            }
        } finally {
            profiles.select(original.id)
            context.deleteDatabase(profileDatabaseName(other.id))
            profiles.remove(other.id)
        }
    }
}
