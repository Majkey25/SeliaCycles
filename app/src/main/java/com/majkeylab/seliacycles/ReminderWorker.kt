package com.majkeylab.seliacycles

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

internal fun shouldNotifyPeriod(
    daysUntil: Int,
    reminderDays: Int,
    periodKey: Long,
    lastNotifiedKey: Long?,
): Boolean = daysUntil in 0..reminderDays && periodKey != lastNotifiedKey

class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        val (backup, snapshots) = CycleStore(applicationContext).use { store ->
            store.load() to store.loadForecastSnapshots().associateBy(ForecastSnapshot::month)
        }
        if (!backup.settings.reminderEnabled || !backup.settings.canPredictPeriods) return Result.success()

        val next = CycleInsights.forDate(backup, snapshots).nextPeriodStart ?: return Result.success()
        val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), next).toInt()
        val reminderState = applicationContext.getSharedPreferences(REMINDER_STATE, Context.MODE_PRIVATE)
        val lastNotified = reminderState.getLong(LAST_NOTIFIED_PERIOD, Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE }
        if (!shouldNotifyPeriod(daysUntil, backup.settings.reminderDays, next.toEpochDay(), lastNotified)) return Result.success()

        createChannel()
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.reminder_title))
            .setContentText(if (daysUntil == 0) {
                applicationContext.getString(R.string.predicted_today)
            } else {
                applicationContext.resources.getQuantityString(R.plurals.reminder_text, daysUntil, daysUntil)
            })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        reminderState.edit { putLong(LAST_NOTIFIED_PERIOD, next.toEpochDay()) }
        return Result.success()
    }

    private fun createChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { lockscreenVisibility = Notification.VISIBILITY_PRIVATE })
    }

    companion object {
        private const val WORK_NAME = "selia-cycles-period-reminder"
        private const val CHANNEL_ID = "period-reminders"
        private const val NOTIFICATION_ID = 1_210
        private const val REMINDER_STATE = "reminder-state"
        private const val LAST_NOTIFIED_PERIOD = "last-notified-period"

        fun sync(context: Context, settings: AppSettings) {
            val workManager = WorkManager.getInstance(context)
            if (!settings.reminderEnabled || !settings.canPredictPeriods) {
                workManager.cancelUniqueWork(WORK_NAME)
                context.getSharedPreferences(REMINDER_STATE, Context.MODE_PRIVATE).edit {
                    remove(LAST_NOTIFIED_PERIOD)
                }
                return
            }
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(Duration.ofHours(1))
                .build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
