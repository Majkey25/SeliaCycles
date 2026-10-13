package com.majkeylab.seliacycles

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class ReminderDeliveryTest {
    @Test fun periodWorkerDeliversOnceAndDoesNotConsumeBlockedReminder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val manager = context.getSystemService(NotificationManager::class.java)
        check(manager.areNotificationsEnabled()) { "Grant notifications to the disposable QA install before running this test" }
        // Android cannot raise this channel again from app code; use a disposable QA install.
        check(manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE) {
            "The QA reminder channel must be unblocked before running this test"
        }
        val profiles = LocalProfiles(context)
        val profile = profiles.create("Reminder delivery QA")
        val workManager = WorkManager.getInstance(context)
        val preferences = context.getSharedPreferences(reminderStateName(profile.id), Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val expectedPeriod = today.plusDays(2)
        val backup = CycleBackup(
            logs = listOf(DayLog(today.minusDays(26), bleeding = true, flow = Flow.MEDIUM)),
            settings = AppSettings(reminderEnabled = true, reminderDays = 14),
        )
        try {
            CycleStore(context, profile.id).use { it.replace(backup) }
            runReminder(workManager, profile.id)
            await { manager.activeNotifications.any { it.tag == profile.id && it.id == NOTIFICATION } }
            assertEquals(expectedPeriod.toEpochDay(), preferences.getLong(LAST_PERIOD, Long.MIN_VALUE))

            manager.cancel(profile.id, NOTIFICATION)
            await { manager.activeNotifications.none { it.tag == profile.id } }
            runReminder(workManager, profile.id)
            assertFalse(manager.activeNotifications.any { it.tag == profile.id })
            assertEquals(expectedPeriod.toEpochDay(), preferences.getLong(LAST_PERIOD, Long.MIN_VALUE))

            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Period reminders", NotificationManager.IMPORTANCE_NONE))
            assertEquals(NotificationManager.IMPORTANCE_NONE, requireNotNull(manager.getNotificationChannel(CHANNEL)).importance)
            check(preferences.edit().remove(LAST_PERIOD).commit())
            runReminder(workManager, profile.id)
            assertFalse("Blocked delivery must not consume the period reminder", preferences.contains(LAST_PERIOD))
            assertFalse(manager.activeNotifications.any { it.tag == profile.id })
        } finally {
            workManager.cancelAllWorkByTag(profile.id).result.get(10, TimeUnit.SECONDS)
            synchronized(LocalProfiles.lock) {
                manager.cancel(profile.id, NOTIFICATION)
                context.deleteSharedPreferences(reminderStateName(profile.id))
                if (context.getDatabasePath(profileDatabaseName(profile.id)).exists()) {
                    check(context.deleteDatabase(profileDatabaseName(profile.id)))
                }
                profiles.remove(profile.id)
            }
        }
    }

    @Test fun periodWorkerDoesNotRecreateMissingOrDeletedProfileData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val profiles = LocalProfiles(context)
        val profile = profiles.create("Missing reminder data QA")
        val database = context.getDatabasePath(profileDatabaseName(profile.id))
        val workManager = WorkManager.getInstance(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val preferences = context.getSharedPreferences(reminderStateName(profile.id), Context.MODE_PRIVATE)
        try {
            assertFalse(database.exists())
            runReminder(workManager, profile.id)
            assertFalse("A registered profile with missing data must not get a new database", database.exists())
            assertFalse(preferences.contains(LAST_PERIOD))
            assertFalse(manager.activeNotifications.any { it.tag == profile.id })

            profiles.remove(profile.id)
            runReminder(workManager, profile.id)
            assertFalse("A deleted profile must not get a new database", database.exists())
            assertFalse(preferences.contains(LAST_PERIOD))
            assertFalse(manager.activeNotifications.any { it.tag == profile.id })
        } finally {
            workManager.cancelAllWorkByTag(profile.id).result.get(10, TimeUnit.SECONDS)
            synchronized(LocalProfiles.lock) {
                manager.cancel(profile.id, NOTIFICATION)
                context.deleteSharedPreferences(reminderStateName(profile.id))
                if (database.exists()) check(context.deleteDatabase(profileDatabaseName(profile.id)))
                if (profiles.profiles().any { it.id == profile.id }) profiles.remove(profile.id)
            }
        }
    }

    private fun runReminder(workManager: WorkManager, profileId: String) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(ReminderWorker.PROFILE_ID_EXTRA to profileId))
            .addTag(profileId)
            .build()
        workManager.enqueue(request).result.get(10, TimeUnit.SECONDS)
        await { workManager.getWorkInfoById(request.id).get(10, TimeUnit.SECONDS)?.state?.isFinished == true }
        assertEquals(WorkInfo.State.SUCCEEDED, workManager.getWorkInfoById(request.id).get(10, TimeUnit.SECONDS)?.state)
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 20_000
        while (!condition()) {
            check(SystemClock.elapsedRealtime() < deadline) { "Timed out waiting for reminder delivery" }
            Thread.sleep(50)
        }
    }

    private companion object {
        const val CHANNEL = "period-reminders"
        const val NOTIFICATION = 1_210
        const val LAST_PERIOD = "last-notified-period"
    }
}
