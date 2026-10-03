package com.majkeylab.seliacycles

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun nextContraceptionReminder(settings: AppSettings, now: ZonedDateTime, handledToday: Boolean,
    pendingDay: LocalDate? = null): ZonedDateTime? {
    if (!settings.contraceptionReminderEnabled || settings.profile.lifeSituation != LifeSituation.HORMONAL_CONTRACEPTION) return null
    val time = LocalTime.ofSecondOfDay(settings.contraceptionReminderMinute * 60L)
    val today = now.toLocalDate().atTime(time).atZone(now.zone)
    return if (handledToday || pendingDay != now.toLocalDate() && !today.isAfter(now)) {
        now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    } else today
}

/** One local alarm per profile. Re-read settings at delivery; alarms never record a dose. */
@SuppressLint("UseKtx") // Check synchronous commit results; KTX edit discards the result.
class ContraceptionReminder : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profiles = LocalProfiles(context).profiles()
                val id = intent.getStringExtra(ReminderWorker.PROFILE_ID_EXTRA)
                if (intent.action == ACTION_REMIND && profiles.any { it.id == id }) {
                    val profileId = requireNotNull(id)
                    val backup = CycleStore(context, profileId).use { it.load() }
                    deliver(context, profileId, backup, intent)
                    sync(context, backup, profileId)
                } else if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
                        Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED,
                        AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
                    profiles.forEach { profile ->
                        CycleStore(context, profile.id).use { sync(context, it.load(), profile.id) }
                    }
                }
            } catch (error: Exception) {
                Log.e("SeliaCycles", "Contraception reminder failed", error)
            } finally { pending.finish() }
        }
    }

    companion object {
        private const val ACTION_REMIND = "com.majkeylab.seliacycles.CONTRACEPTION_REMINDER"
        private const val CHANNEL = "contraception-reminders"
        private const val NOTIFICATION = 1211
        private const val LAST_DAY = "last_day"
        private const val DAY = "scheduled_day"
        private const val MINUTE = "scheduled_minute"
        private const val PENDING_DAY = "pending_day"
        private const val PENDING_MINUTE = "pending_minute"

        fun exactAllowed(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

        private fun preferences(context: Context, profileId: String) =
            context.getSharedPreferences("contraception-reminder-$profileId", Context.MODE_PRIVATE)

        private fun alarmIntent(context: Context, profileId: String) = Intent(context, ContraceptionReminder::class.java)
            .setAction(ACTION_REMIND).setData("selia://contraception/$profileId".toUri())
            .putExtra(ReminderWorker.PROFILE_ID_EXTRA, profileId)

        fun cancel(context: Context, profileId: String, clearHistory: Boolean = false) {
            requireValidProfileId(profileId)
            PendingIntent.getBroadcast(context, 0, alarmIntent(context, profileId),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                context.getSystemService(AlarmManager::class.java).cancel(it)
                it.cancel()
            }
            NotificationManagerCompat.from(context).cancel(profileId, NOTIFICATION)
            val edit = preferences(context, profileId).edit().remove(PENDING_DAY).remove(PENDING_MINUTE)
            if (clearHistory) edit.clear()
            check(edit.commit())
        }

        @SuppressLint("ScheduleExactAlarm") // Permission checked; revoked access falls back to an inexact alarm.
        fun sync(context: Context, backup: CycleBackup, profileId: String) {
            requireValidProfileId(profileId)
            val now = ZonedDateTime.now()
            val log = backup.logs.firstOrNull { it.day == now.toLocalDate() }
            val handled = log?.contraception != null ||
                preferences(context, profileId).getLong(LAST_DAY, Long.MIN_VALUE) == now.toLocalDate().toEpochDay()
            val prefs = preferences(context, profileId)
            val pendingDay = prefs.getLong(PENDING_DAY, Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE }
                ?.takeIf { prefs.getInt(PENDING_MINUTE, -1) == backup.settings.contraceptionReminderMinute }
                ?.let(LocalDate::ofEpochDay)
            val next = nextContraceptionReminder(backup.settings, now, handled, pendingDay)
            if (next == null || !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                cancel(context, profileId)
                return
            }
            if (log?.contraception != null) NotificationManagerCompat.from(context).cancel(profileId, NOTIFICATION)
            val intent = alarmIntent(context, profileId).putExtra(DAY, next.toLocalDate().toEpochDay())
                .putExtra(MINUTE, backup.settings.contraceptionReminderMinute)
            val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val manager = context.getSystemService(AlarmManager::class.java)
            val at = next.toInstant().toEpochMilli()
            try {
                if (exactAllowed(context)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } catch (_: SecurityException) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
            check(prefs.edit().putLong(PENDING_DAY, next.toLocalDate().toEpochDay())
                .putInt(PENDING_MINUTE, backup.settings.contraceptionReminderMinute).commit())
        }

        private fun deliver(context: Context, profileId: String, backup: CycleBackup, intent: Intent) {
            val today = LocalDate.now()
            val settings = backup.settings
            if (!settings.contraceptionReminderEnabled || settings.profile.lifeSituation != LifeSituation.HORMONAL_CONTRACEPTION ||
                intent.getLongExtra(DAY, Long.MIN_VALUE) != today.toEpochDay() ||
                intent.getIntExtra(MINUTE, -1) != settings.contraceptionReminderMinute ||
                LocalTime.now().toSecondOfDay() < settings.contraceptionReminderMinute * 60 ||
                backup.logs.any { it.day == today && it.contraception != null } ||
                preferences(context, profileId).getLong(LAST_DAY, Long.MIN_VALUE) == today.toEpochDay()) return
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) return
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.contraception_reminder), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE })
            val open = Intent(context, MainActivity::class.java).setData("selia://local-profile/$profileId".toUri())
                .putExtra(ReminderWorker.PROFILE_ID_EXTRA, profileId)
            val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.contraception_reminder))
                .setContentText(context.getString(R.string.contraception_reminder_body))
                .setContentIntent(PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
            NotificationManagerCompat.from(context).notify(profileId, NOTIFICATION, notification)
            check(preferences(context, profileId).edit().putLong(LAST_DAY, today.toEpochDay()).commit())
        }
    }
}
