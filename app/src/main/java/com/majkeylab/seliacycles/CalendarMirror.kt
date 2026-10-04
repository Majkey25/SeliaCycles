package com.majkeylab.seliacycles

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.CalendarContract
import android.util.AtomicFile
import androidx.core.content.ContextCompat
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

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
    val calendarSyncEnabled: Boolean = false,
)

class CalendarCleanupException(cause: Exception) : Exception("Calendar sync is off; cleanup failed", cause)

data class CalendarCleanupEvent(
    val id: Long,
    val calendarId: Long,
    val title: String?,
    val startMillis: Long,
    val endMillis: Long,
    val appPackage: String?,
    val appUri: String?,
    val description: String?,
    val allDay: Int,
    val recurrenceRule: String?,
    val recurrenceDates: String?,
    val legacy: Boolean,
)

data class CalendarCleanupPreview(val profileId: String, val calendar: DeviceCalendar, val events: List<CalendarCleanupEvent>)

internal fun calendarMirrorUriPrefix(profileId: String): String {
    requireValidProfileId(profileId)
    return if (profileId == LocalProfiles.DEFAULT_ID) "selia://calendar-mirror/"
    else "selia://profile-calendar-mirror/$profileId/"
}

internal fun calendarMirrorSelectionFile(profileId: String): String {
    requireValidProfileId(profileId)
    return if (profileId == LocalProfiles.DEFAULT_ID) "calendar-mirror-id" else "calendar-mirror-id-$profileId"
}

class CalendarMirror(private val context: Context, private val profileId: String = LocalProfiles.DEFAULT_ID) {
    private val resolver: ContentResolver = context.contentResolver
    private val customUriPrefix = calendarMirrorUriPrefix(profileId)
    private val selectionFile = File(context.noBackupFilesDir, calendarMirrorSelectionFile(profileId))
    private val enabledFile = AtomicFile(File(context.noBackupFilesDir, "${selectionFile.name}-enabled"))
    private val legacyTitles by lazy {
        listOf("en", "cs", "sk", "de", "pl", "es").flatMap { language ->
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            })
            EVENT_TITLES.map(localized::getString)
        }.toSet()
    }

    fun snapshot(backup: CycleBackup, snapshots: Map<java.time.YearMonth, ForecastSnapshot>): CalendarMirrorSnapshot = synchronized(lock) {
        val selectedId = selectedCalendarId()
        val enabled = calendarSyncEnabled()
        if (!hasPermissions()) return@synchronized CalendarMirrorSnapshot(false, selectedId, emptyList(), enabled)
        val calendars = writableCalendars()
        if (enabled && selectedId != null && calendars.any { it.id == selectedId }) replaceEvents(selectedId, backup, snapshots)
        CalendarMirrorSnapshot(true, selectedId, calendars, enabled)
    }

    fun connect(
        calendarId: Long,
        backup: CycleBackup,
        snapshots: Map<java.time.YearMonth, ForecastSnapshot>,
    ) = synchronized(lock) {
        check(hasPermissions())
        require(writableCalendars().any { it.id == calendarId })
        saveSyncEnabled(false)
        saveSelectedCalendarId(calendarId)
        try {
            saveSyncEnabled(true)
            replaceEvents(calendarId, backup, snapshots)
        } catch (failure: Exception) {
            saveSyncEnabled(false)
            throw failure
        }
    }

    fun disconnect() = synchronized(lock) {
        // Persist OFF before any permission check or provider operation. Never roll this back.
        saveSyncEnabled(false)
        try {
            check(hasPermissions())
            replaceEvents(null, CycleBackup(), emptyMap())
            saveSelectedCalendarId(null)
        } catch (failure: Exception) {
            throw CalendarCleanupException(failure)
        }
    }

    fun previewCleanup(calendarId: Long): CalendarCleanupPreview = synchronized(lock) {
        saveSyncEnabled(false)
        try {
            check(hasPermissions())
            CalendarCleanupPreview(profileId, writableCalendars().first { it.id == calendarId }, cleanupCandidates(calendarId))
        } catch (failure: Exception) {
            throw CalendarCleanupException(failure)
        }
    }

    fun deletePreviewedEvents(preview: CalendarCleanupPreview, selectedIds: Set<Long>) = synchronized(lock) {
        saveSyncEnabled(false)
        try {
            check(hasPermissions())
            require(preview.profileId == profileId && selectedIds.isNotEmpty())
            require(selectedIds.all { id -> preview.events.any { it.id == id } })
            require(writableCalendars().any { it.id == preview.calendar.id })
            val current = cleanupCandidates(preview.calendar.id).associateBy(CalendarCleanupEvent::id)
            val approved = preview.events.filter { it.id in selectedIds }
            check(approved.all { current[it.id] == it }) { "Calendar events changed; review again" }
            approved.chunked(100).forEach { batch ->
                val operations = ArrayList(batch.map { event ->
                    // Guard the row again at deletion, including nullable metadata and the calendar ID.
                    val fields = listOf(
                        CalendarContract.Events.CALENDAR_ID to event.calendarId.toString(),
                        CalendarContract.Events.TITLE to event.title,
                        CalendarContract.Events.DTSTART to event.startMillis.toString(),
                        CalendarContract.Events.DTEND to event.endMillis.toString(),
                        CalendarContract.Events.ALL_DAY to event.allDay.toString(),
                        CalendarContract.Events.CUSTOM_APP_PACKAGE to event.appPackage,
                        CalendarContract.Events.CUSTOM_APP_URI to event.appUri,
                        CalendarContract.Events.DESCRIPTION to event.description,
                        CalendarContract.Events.RRULE to event.recurrenceRule,
                        CalendarContract.Events.RDATE to event.recurrenceDates,
                        CalendarContract.Events.DELETED to "0",
                    )
                    ContentProviderOperation.newDelete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id))
                        .withSelection(fields.joinToString(" AND ") { (column, value) ->
                            if (value == null) "$column IS NULL" else "$column = ?"
                        }, fields.mapNotNull { it.second }.toTypedArray())
                        .withExpectedCount(1).build()
                })
                val results = resolver.applyBatch(CalendarContract.AUTHORITY, operations)
                check(results.size == batch.size && results.all { it.count == 1 })
            }
            if (existingEvents(emptyList(), null, false).isEmpty()) saveSelectedCalendarId(null)
        } catch (failure: Exception) {
            throw CalendarCleanupException(failure)
        }
    }

    private fun cleanupCandidates(calendarId: Long): List<CalendarCleanupEvent> {
        val titles = legacyTitles.toList()
        return resolver.query(CalendarContract.Events.CONTENT_URI, EVENT_COLUMNS,
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DELETED} = 0 AND " +
                "(((${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? OR ${CalendarContract.Events.CUSTOM_APP_PACKAGE} IS NULL OR ${CalendarContract.Events.CUSTOM_APP_PACKAGE} = '') AND ${CalendarContract.Events.CUSTOM_APP_URI} LIKE ?) OR " +
                "${CalendarContract.Events.DESCRIPTION} LIKE ? OR ${CalendarContract.Events.TITLE} IN (${titles.joinToString { "?" }}))",
            (listOf(calendarId.toString(), context.packageName, "$customUriPrefix%", "%$REFERENCE_PREFIX$profileId:%") + titles).toTypedArray(),
            "${CalendarContract.Events.DTSTART} ASC, ${BaseColumns._ID} ASC",
        )?.use { cursor -> buildList {
            while (cursor.moveToNext()) {
                val title = cursor.getString(3)
                val uri = cursor.getString(1)
                val description = cursor.getString(4)
                val pkg = cursor.getString(12)
                val owned = ownedKey(pkg, uri, description) != null
                val legacy = !owned && (pkg.isNullOrBlank() || pkg == context.packageName) && uri.isNullOrBlank() &&
                    !description.orEmpty().contains(REFERENCE_PREFIX) &&
                    title in legacyTitles && cursor.getInt(8) == 1 && cursor.getLong(6) > cursor.getLong(5) &&
                    cursor.getString(13).isNullOrBlank() && cursor.getString(14).isNullOrBlank()
                if (owned || legacy) {
                    require(size < CycleBackup.MAX_LOGS) { "Too many calendar copies to review at once" }
                    add(CalendarCleanupEvent(cursor.getLong(0), cursor.getLong(2), title, cursor.getLong(5), cursor.getLong(6),
                        pkg, uri, description, cursor.getInt(8), cursor.getString(13), cursor.getString(14), legacy))
                }
            }
        } } ?: error("Calendar provider returned no event cursor")
    }

    private fun ownedKey(pkg: String?, uri: String?, description: String?): String? {
        if (!pkg.isNullOrBlank() && pkg != context.packageName) return null
        // A non-link token survives normal line-break/HTML changes without a Google-specific API.
        val references = description.orEmpty().split(REFERENCE_PREFIX).drop(1)
            .map { it.substringBefore(']', "") }
        if (references.any { !it.startsWith("$profileId:") }) return null
        val markerKey = references.distinct().singleOrNull()?.removePrefix("$profileId:")
        val reference = uri?.takeIf { it.isNotBlank() } ?: markerKey?.let { "$customUriPrefix$it" } ?: return null
        if (!reference.startsWith(customUriPrefix)) return null
        val key = reference.removePrefix(customUriPrefix)
        if (references.isNotEmpty() && references.any { it != "$profileId:$key" }) return null
        val parts = key.split('/', limit = 2)
        if (parts.size != 2 || MirrorEventKind.entries.none { it.name.lowercase() == parts[0] }) return null
        val day = runCatching { LocalDate.parse(parts[1]) }.getOrNull() ?: return null
        return key.takeIf { day in DayLog.MIN_DATE..DayLog.MAX_DATE && day.toString() == parts[1] }
    }

    // Missing, unreadable and legacy state are OFF; an old calendar ID is not consent to resume.
    fun calendarSyncEnabled(): Boolean = synchronized(lock) {
        runCatching { enabledFile.openRead().bufferedReader().use { it.readText() } == "true" }.getOrDefault(false)
    }

    private fun saveSyncEnabled(enabled: Boolean) {
        val output = enabledFile.startWrite()
        try {
            output.write(enabled.toString().toByteArray(Charsets.UTF_8))
            enabledFile.finishWrite(output)
        } catch (failure: Exception) {
            enabledFile.failWrite(output)
            throw failure
        }
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
        check(calendarId == null || calendarSyncEnabled() && selectedCalendarId() == calendarId)
        val desired = calendarId?.let { CalendarMirrorPlanner.plan(backup, snapshots) }.orEmpty()
        val operations = ArrayList<ContentProviderOperation>()
        val existing = existingEvents(desired, calendarId, backup.settings.partnerViewEnabled)
        val descriptions = existing.associate { it.id to it.description }
        CalendarMirrorDiff.plan(desired, existing).forEach { mutation ->
            operations += when (mutation) {
                is MirrorMutation.Insert -> ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                    .withValues(eventValues(requireNotNull(calendarId), mutation.event, backup.settings.partnerViewEnabled))
                    .build()
                is MirrorMutation.Update -> ContentProviderOperation.newUpdate(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, mutation.id),
                ).withValues(eventValues(requireNotNull(calendarId), mutation.event, backup.settings.partnerViewEnabled, descriptions[mutation.id]))
                    .withSelection(if (descriptions[mutation.id] == null) "${CalendarContract.Events.DESCRIPTION} IS NULL"
                        else "${CalendarContract.Events.DESCRIPTION} = ?", descriptions[mutation.id]?.let { arrayOf(it) })
                    .withExpectedCount(1).build()
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
            "${CalendarContract.Events.DELETED} = 0 AND ((" +
                "(${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? OR ${CalendarContract.Events.CUSTOM_APP_PACKAGE} IS NULL OR ${CalendarContract.Events.CUSTOM_APP_PACKAGE} = '') AND ${CalendarContract.Events.CUSTOM_APP_URI} LIKE ?) OR " +
                "${CalendarContract.Events.DESCRIPTION} LIKE ?)",
            arrayOf(context.packageName, "$customUriPrefix%", "%$REFERENCE_PREFIX$profileId:%"),
            null,
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val key = ownedKey(cursor.getString(12), cursor.getString(1), cursor.getString(4)) ?: continue
                    val desiredEvent = desiredByKey[key]
                    val values = desiredEvent?.let {
                        eventValues(requireNotNull(calendarId), it, partnerViewEnabled, cursor.getString(4))
                    }
                    add(StoredMirrorEvent(
                        id = cursor.getLong(0),
                        key = key,
                        current = desiredEvent?.takeIf { values != null && cursor.matches(values) },
                        description = cursor.getString(4),
                    ))
                }
            }
        } ?: error("Calendar provider returned no event cursor")
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
        existingDescription: String? = null,
    ): ContentValues = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, context.getString(when (event.kind) {
            MirrorEventKind.RECORDED -> R.string.calendar_event_recorded
            MirrorEventKind.ESTIMATED -> R.string.calendar_event_estimated
            MirrorEventKind.FERTILE -> R.string.calendar_event_fertile
            MirrorEventKind.OVULATION -> R.string.calendar_event_ovulation
        }))
        val notice = when (event.kind) {
            MirrorEventKind.RECORDED -> ""
            MirrorEventKind.ESTIMATED -> context.getString(R.string.estimate_notice)
            MirrorEventKind.FERTILE, MirrorEventKind.OVULATION -> context.getString(R.string.fertility_estimate_notice)
        }
        val text = existingDescription ?: notice
        val marker = "$REFERENCE_PREFIX$profileId:${event.key}]"
        put(CalendarContract.Events.DESCRIPTION, if (text.contains(marker)) text else {
            listOf(text, marker).filter(String::isNotEmpty).joinToString("\n\n")
        })
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
        if (calendarId == null) {
            check(!selectionFile.exists() || selectionFile.delete())
        } else selectionFile.writeText(calendarId.toString())
    }

    private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    companion object {
        // ponytail: one process-wide lock prevents stale instances writing after OFF; split by profile only if contention matters.
        private val lock = Any()
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        private const val UTC = "UTC"
        private const val REFERENCE_PREFIX = "[SeliaCycles:"
        private val EVENT_TITLES = listOf(R.string.calendar_event_recorded, R.string.calendar_event_estimated,
            R.string.calendar_event_fertile, R.string.calendar_event_ovulation)
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
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.RDATE,
        )
    }
}
