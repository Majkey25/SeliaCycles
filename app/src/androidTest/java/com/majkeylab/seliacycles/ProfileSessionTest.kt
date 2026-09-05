package com.majkeylab.seliacycles

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSessionTest {
    @Test
    fun failedProfileReadBlocksWritesUntilExplicitRetrySucceeds() = runBlocking {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        check(application.packageName.endsWith(".qa")) { "Tests must use the isolated QA application" }
        val profiles = LocalProfiles(application)
        val original = profiles.selected()
        val damaged = profiles.create("Read failure test")
        val today = LocalDate.now()
        CycleStore(application, damaged.id).use {
            it.saveLog(DayLog(today, note = "Preserve existing data"))
            it.saveSettings(AppSettings(cycleLengthOverride = 43))
            it.writableDatabase.execSQL("UPDATE settings SET theme = 'INVALID_TEST_THEME' WHERE id = 1")
        }
        val viewModels = ViewModelStore()
        val viewModel = withContext(Dispatchers.Main) {
            MainViewModel(application).also { viewModels.put("read-failure-test", it) }
        }
        suspend fun awaitProfile(id: String, failed: Boolean = false) = withTimeout(15_000) {
            viewModel.state.first { !it.loading && !it.busy && it.activeProfile.id == id && it.loadFailed == failed }
        }
        try {
            awaitProfile(original.id)
            withContext(Dispatchers.Main) { viewModel.selectProfile(damaged.id) }
            awaitProfile(damaged.id, failed = true)
            withContext(Dispatchers.Main) {
                viewModel.saveSettings(AppSettings())
                viewModel.saveLog(DayLog(today, note = "Must not overwrite")).join()
                viewModel.clearAll().join()
                assertTrue(viewModel.state.value.loadFailed)
                assertEquals(R.string.operation_failed, viewModel.state.value.message)
                viewModel.retryLoad()
                assertTrue(viewModel.state.value.loading)
                assertFalse(viewModel.state.value.loadFailed)
            }
            awaitProfile(damaged.id, failed = true)
            CycleStore(application, damaged.id).use { store ->
                store.readableDatabase.rawQuery("SELECT theme, cycle_length_override FROM settings WHERE id = 1", null).use {
                    check(it.moveToFirst())
                    assertEquals("INVALID_TEST_THEME", it.getString(0))
                    assertEquals(43, it.getInt(1))
                }
                store.writableDatabase.execSQL("UPDATE settings SET theme = ? WHERE id = 1", arrayOf(AppTheme.SYSTEM.name))
            }
            withContext(Dispatchers.Main) { viewModel.retryLoad() }
            awaitProfile(damaged.id)
            assertEquals(43, viewModel.state.value.backup.settings.cycleLengthOverride)
            assertEquals("Preserve existing data", viewModel.state.value.backup.logs.single().note)
        } finally {
            withContext(Dispatchers.Main) { viewModel.selectProfile(original.id) }
            awaitProfile(original.id)
            withContext(Dispatchers.Main) { viewModels.clear() }
            ReminderWorker.cancel(application, damaged.id)
            application.deleteDatabase(profileDatabaseName(damaged.id))
            profiles.remove(damaged.id)
        }
    }

    @Test
    fun rapidSwitchesAndStaleCallbacksKeepProfileDataIsolated() = runBlocking {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        check(application.packageName.endsWith(".qa")) { "Tests must use the isolated QA application" }
        val profiles = LocalProfiles(application)
        val original = profiles.selected()
        val first = profiles.create("Session A")
        val second = profiles.create("Session B")
        val today = LocalDate.now()
        val viewModels = ViewModelStore()
        val output = File.createTempFile("stale-profile-export", ".txt", application.cacheDir)
        output.writeText("untouched")
        val viewModel = withContext(Dispatchers.Main) {
            MainViewModel(application).also { viewModels.put("session-test", it) }
        }
        suspend fun awaitProfile(id: String) = withTimeout(15_000) {
            viewModel.state.first { !it.loading && !it.busy && it.activeProfile.id == id }
        }
        try {
            awaitProfile(original.id)
            withContext(Dispatchers.Main) { viewModel.selectProfile(first.id) }
            awaitProfile(first.id)
            withContext(Dispatchers.Main) { viewModel.saveLog(DayLog(today, note = "A only")) }
            awaitProfile(first.id)
            withContext(Dispatchers.Main) {
                viewModel.selectProfile(second.id)
                viewModel.saveLog(DayLog(today, note = "Rejected during switch"))
                viewModel.selectProfile(first.id)
                viewModel.selectProfile(second.id)
            }
            awaitProfile(second.id)
            assertEquals(emptyList<DayLog>(), viewModel.state.value.backup.logs)
            withContext(Dispatchers.Main) { viewModel.saveLog(DayLog(today, note = "B only")) }
            awaitProfile(second.id)
            withContext(Dispatchers.Main) {
                viewModel.inspectMyCalendar(Uri.fromFile(output), first.id).join()
                assertEquals(R.string.operation_failed, viewModel.state.value.message)
                assertEquals(null, viewModel.state.value.myCalendarPreview)
                viewModel.exportMyCalendar(Uri.fromFile(output), first.id).join()
                assertEquals(R.string.operation_failed, viewModel.state.value.message)
                val settings = viewModel.state.value.backup.settings
                viewModel.saveSettings(settings.copy(reminderEnabled = true), first.id)
                assertEquals(settings, viewModel.state.value.backup.settings)
                viewModel.inspectMyCalendar(Uri.fromFile(output), null).join()
                assertEquals(R.string.operation_failed, viewModel.state.value.message)
                viewModel.selectProfile("not-a-profile")
                assertEquals(second.id, viewModel.state.value.activeProfile.id)
            }
            assertEquals("untouched", output.readText())
            CycleStore(application, first.id).use { assertEquals("A only", it.load().logs.single().note) }
            CycleStore(application, second.id).use { assertEquals("B only", it.load().logs.single().note) }
        } finally {
            withContext(Dispatchers.Main) { viewModel.selectProfile(original.id) }
            awaitProfile(original.id)
            withContext(Dispatchers.Main) { viewModels.clear() }
            listOf(first, second).forEach {
                ReminderWorker.cancel(application, it.id)
                application.deleteDatabase(profileDatabaseName(it.id))
                profiles.remove(it.id)
            }
            output.delete()
        }
    }
}
