package com.majkeylab.seliacycles

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
)

data class CalendarMirrorSnapshot(
    val permissionGranted: Boolean,
    val selectedCalendarId: Long?,
    val calendars: List<DeviceCalendar>,
)

internal fun calendarMirrorUriPrefix(profileId: String): String {
    requireValidProfileId(profileId)
    return if (profileId == LocalProfiles.DEFAULT_ID) "selia://calendar-mirror/"
    else "selia://profile-calendar-mirror/$profileId/"
}

internal fun calendarMirrorSelectionFile(profileId: String): String {
    requireValidProfileId(profileId)
    return if (profileId == LocalProfiles.DEFAULT_ID) "calendar-mirror-id" else "calendar-mirror-id-$profileId"
}

class CalendarMirror(private val context: Context, profileId: String = LocalProfiles.DEFAULT_ID) {
    private val resolver: ContentResolver = context.contentResolver
    private val customUriPrefix = calendarMirrorUriPrefix(profileId)
    private val selectionFile = File(context.noBackupFilesDir, calendarMirrorSelectionFile(profileId))

    fun snapshot(backup: CycleBackup, snapshots: Map<java.time.YearMonth, ForecastSnapshot>): CalendarMirrorSnapshot {
        val selectedId = selectedCalendarId()
        if (!hasPermissions()) return CalendarMirrorSnapshot(false, selectedId, emptyList())
        val calendars = writableCalendars()
        if (selectedId != null && calendars.any { it.id == selectedId }) replaceEvents(selectedId, backup, snapshots)
        return CalendarMirrorSnapshot(true, selectedId, calendars)
    }

    fun connect(
        calendarId: Long,
        backup: CycleBackup,
        snapshots: Map<java.time.YearMonth, ForecastSnapshot>,
    ) {
        check(hasPermissions())
        require(writableCalendars().any { it.id == calendarId })
        val previousId = selectedCalendarId()
        saveSelectedCalendarId(calendarId)
        try {
            replaceEvents(calendarId, backup, snapshots)
        } catch (failure: Exception) {
            saveSelectedCalendarId(previousId)
            throw failure
        }
    }

    fun disconnect() {
        check(hasPermissions())
        replaceEvents(null, CycleBackup(), emptyMap())
        saveSelectedCalendarId(null)
    }

    fun selectedCalendarId(): Long? = runCatching { selectionFile.readText().trim().toLong() }
        .getOrNull()
        ?.takeIf { it > 0 }

    fun hasPermissions(): Boolean = REQUIRED_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun writableCalendars(): List<DeviceCalendar> = resolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        CALENDAR_COLUMNS,
        "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND " +
            "${CalendarContract.Calendars.SYNC_EVENTS} = 1 AND ${CalendarContract.Calendars.VISIBLE} = 1",
        arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
        "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} COLLATE NOCASE ASC",
    )?.use { cursor ->
        buildList(cursor.count) {
            while (cursor.moveToNext()) {
                add(
                    DeviceCalendar(
                        id = cursor.getLong(0),
                        displayName = cursor.getString(1).orEmpty(),
                        accountName = cursor.getString(2).orEmpty(),
                        color = cursor.getInt(3),
                    ),
                )
            }
        }
    }.orEmpty()

    private fun replaceEvents(
        calendarId: Long?,
        backup: CycleBackup,
        snapshots: Map<java.time.YearMonth, ForecastSnapshot>,
    ) {
        val desired = calendarId?.let { CalendarMirrorPlanner.plan(backup, snapshots) }.orEmpty()
        val operations = ArrayList<ContentProviderOperation>()
        CalendarMirrorDiff.plan(
            desired,
            existingEvents(desired, calendarId, backup.settings.partnerViewEnabled),
        ).forEach { mutation ->
            operations += when (mutation) {
                is MirrorMutation.Insert -> ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                    .withValues(eventValues(requireNotNull(calendarId), mutation.event, backup.settings.partnerViewEnabled))
                    .build()
                is MirrorMutation.Update -> ContentProviderOperation.newUpdate(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, mutation.id),
                ).withValues(eventValues(requireNotNull(calendarId), mutation.event, backup.settings.partnerViewEnabled)).build()
                is MirrorMutation.Delete -> ContentProviderOperation.newDelete(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, mutation.id),
                ).build()
            }
        }
        if (operations.isEmpty()) return
        check(resolver.applyBatch(CalendarContract.AUTHORITY, operations).size == operations.size)
    }

    private fun existingEvents(
        desired: List<MirrorEvent>,
        calendarId: Long?,
        partnerViewEnabled: Boolean,
    ): List<StoredMirrorEvent> {
        val desiredByKey = desired.associateBy(MirrorEvent::key)
        return resolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_COLUMNS,
            "${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} LIKE ?",
            arrayOf(context.packageName, "$customUriPrefix%"),
            null,
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val uri = cursor.getString(1).orEmpty()
                    val key = uri.removePrefix(customUriPrefix)
                    val desiredEvent = desiredByKey[key]
                    val values = desiredEvent?.let {
                        eventValues(requireNotNull(calendarId), it, partnerViewEnabled)
                    }
                    add(StoredMirrorEvent(
                        id = cursor.getLong(0),
                        key = key,
                        current = desiredEvent?.takeIf { values != null && cursor.matches(values) },
                    ))
                }
            }
        }.orEmpty()
    }

    private fun Cursor.matches(values: ContentValues): Boolean =
        getLong(2) == values.getAsLong(CalendarContract.Events.CALENDAR_ID) &&
            getString(3).orEmpty() == values.getAsString(CalendarContract.Events.TITLE).orEmpty() &&
            (!values.containsKey(CalendarContract.Events.DESCRIPTION) ||
                getString(4).orEmpty() == values.getAsString(CalendarContract.Events.DESCRIPTION).orEmpty()) &&
            getLong(5) == values.getAsLong(CalendarContract.Events.DTSTART) &&
            getLong(6) == values.getAsLong(CalendarContract.Events.DTEND) &&
            getString(7).orEmpty() == values.getAsString(CalendarContract.Events.EVENT_TIMEZONE).orEmpty() &&
            getInt(8) == values.getAsInteger(CalendarContract.Events.ALL_DAY) &&
            getInt(9) == values.getAsInteger(CalendarContract.Events.AVAILABILITY) &&
            getInt(10) == values.getAsInteger(CalendarContract.Events.ACCESS_LEVEL) &&
            getInt(11) == values.getAsInteger(CalendarContract.Events.STATUS)

    private fun eventValues(
        calendarId: Long,
        event: MirrorEvent,
        partnerViewEnabled: Boolean,
    ): ContentValues = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, context.getString(when (event.kind) {
            MirrorEventKind.RECORDED -> R.string.calendar_event_recorded
            MirrorEventKind.ESTIMATED -> R.string.calendar_event_estimated
            MirrorEventKind.FERTILE -> R.string.calendar_event_fertile
            MirrorEventKind.OVULATION -> R.string.calendar_event_ovulation
        }))
        when (event.kind) {
            MirrorEventKind.RECORDED -> Unit
            MirrorEventKind.ESTIMATED -> put(CalendarContract.Events.DESCRIPTION, context.getString(R.string.estimate_notice))
            MirrorEventKind.FERTILE, MirrorEventKind.OVULATION ->
                put(CalendarContract.Events.DESCRIPTION, context.getString(R.string.fertility_estimate_notice))
        }
        put(CalendarContract.Events.DTSTART, event.start.utcMillis())
        put(CalendarContract.Events.DTEND, event.endExclusive.utcMillis())
        put(CalendarContract.Events.EVENT_TIMEZONE, UTC)
        put(CalendarContract.Events.ALL_DAY, 1)
        put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
        put(
            CalendarContract.Events.ACCESS_LEVEL,
            if (partnerViewEnabled) CalendarContract.Events.ACCESS_DEFAULT else CalendarContract.Events.ACCESS_PRIVATE,
        )
        put(CalendarContract.Events.STATUS, if (event.kind == MirrorEventKind.RECORDED) {
            CalendarContract.Events.STATUS_CONFIRMED
        } else {
            CalendarContract.Events.STATUS_TENTATIVE
        })
        put(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
        put(CalendarContract.Events.CUSTOM_APP_URI, "$customUriPrefix${event.kind.name.lowercase()}/${event.start}")
    }

    private fun saveSelectedCalendarId(calendarId: Long?) {
        if (calendarId == null) selectionFile.delete() else selectionFile.writeText(calendarId.toString())
    }

    private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        private const val UTC = "UTC"
        private val CALENDAR_COLUMNS = arrayOf(
            BaseColumns._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        private val EVENT_COLUMNS = arrayOf(
            BaseColumns._ID,
            CalendarContract.Events.CUSTOM_APP_URI,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_TIMEZONE,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.AVAILABILITY,
            CalendarContract.Events.ACCESS_LEVEL,
            CalendarContract.Events.STATUS,
        )
    }
}
