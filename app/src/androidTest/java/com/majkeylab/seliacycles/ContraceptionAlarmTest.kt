package com.majkeylab.seliacycles

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class ContraceptionAlarmTest {
    @Test fun nativeAlarmNotifiesAndDisabledProfileRejectsQueuedDelivery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        assumeTrue(ContraceptionReminder.exactAllowed(context))
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())
        val profiles = LocalProfiles(context)
        val profile = profiles.create("Alarm QA")
        val next = LocalTime.now().plusMinutes(1)
        val settings = AppSettings(profile = UserProfile(lifeSituation = LifeSituation.HORMONAL_CONTRACEPTION),
            contraceptionReminderEnabled = true, contraceptionReminderMinute = next.hour * 60 + next.minute)
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            CycleStore(context, profile.id).use { it.saveSettings(settings) }
            ContraceptionReminder.sync(context, CycleBackup(settings = settings), profile.id)
            val deadline = SystemClock.elapsedRealtime() + 90_000
            while (SystemClock.elapsedRealtime() < deadline && manager.activeNotifications.none { it.tag == profile.id }) {
                Thread.sleep(250)
            }
            assertEquals("Expected one actual Android notification", 1, manager.activeNotifications.count { it.tag == profile.id })
            val disabled = settings.copy(contraceptionReminderEnabled = false)
            CycleStore(context, profile.id).use { it.saveSettings(disabled) }
            ContraceptionReminder.sync(context, CycleBackup(settings = disabled), profile.id)
            assertFalse(manager.activeNotifications.any { it.tag == profile.id })
            val queued = Intent(context, ContraceptionReminder::class.java)
                .setAction("com.majkeylab.seliacycles.CONTRACEPTION_REMINDER")
                .setData("selia://contraception/${profile.id}".toUri())
                .putExtra(ReminderWorker.PROFILE_ID_EXTRA, profile.id)
                .putExtra("scheduled_day", LocalDate.now().toEpochDay())
                .putExtra("scheduled_minute", settings.contraceptionReminderMinute)
            assertNull(PendingIntent.getBroadcast(context, 0, queued, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
            val delivered = CountDownLatch(1)
            context.sendOrderedBroadcast(queued, null, object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) { delivered.countDown() }
            }, Handler(Looper.getMainLooper()), 0, null, null)
            assertTrue(delivered.await(10, TimeUnit.SECONDS))
            assertFalse(manager.activeNotifications.any { it.tag == profile.id })
        } finally {
            ContraceptionReminder.cancel(context, profile.id, clearHistory = true)
            context.deleteDatabase(profileDatabaseName(profile.id))
            profiles.remove(profile.id)
        }
    }
}
