package com.majkeylab.seliacycles

import android.content.Context
import androidx.lifecycle.ViewModelStore
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LocalProfilesPreferencesTest {
    @Test
    fun `missing known and unknown modes preserve profiles and stored data`() {
        val context = RuntimeEnvironment.getApplication()
        val profiles = LocalProfiles(context)
        val profile = profiles.create("Anna", icon = ProfileIcon.STAR)
        profiles.select(profile.id)
        val preferences = context.getSharedPreferences("local_profiles", Context.MODE_PRIVATE)
        val log = DayLog(LocalDate.of(2026, 10, 20), note = "Preserve saved data")
        CycleStore(context, profile.id).use { it.saveLog(log) }

        val modes = listOf(null to UiMode.STANDARD) + UiMode.entries.map { it.name to it } +
            ("UNKNOWN_MODE" to UiMode.STANDARD)
        for ((stored, expected) in modes) {
            check(preferences.edit().putString("mode_${profile.id}", stored).commit())
            val reopened = LocalProfiles(context)
            assertEquals(profile.copy(mode = expected), reopened.selected())
            assertEquals(2, reopened.profiles().size)
            assertEquals(stored, preferences.getString("mode_${profile.id}", null))
            CycleStore(context, profile.id).use { assertEquals(listOf(log), it.load().logs) }
        }

        check(preferences.edit().putString("mode_default", "UNKNOWN_MODE").commit())
        assertEquals(UiMode.STANDARD, LocalProfiles(context).profiles().first().mode)
        val models = ViewModelStore()
        try {
            val model = MainViewModel(context).also { models.put("cold-start", it) }
            assertEquals(profile.copy(mode = UiMode.STANDARD), model.state.value.activeProfile)
        } finally {
            models.clear()
        }
        CycleStore(context, profile.id).use { assertEquals(listOf(log), it.load().logs) }
    }
}
