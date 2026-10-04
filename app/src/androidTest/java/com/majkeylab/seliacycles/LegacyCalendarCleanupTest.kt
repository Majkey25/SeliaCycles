package com.majkeylab.seliacycles

import android.content.ContentProvider
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.CalendarContract.Events
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class LegacyCalendarCleanupTest {
    @Test fun legacyCopiesWithoutProviderMetadataAreDeletedAndStayOffAfterRestart() = withCalendar { context, provider ->
        val legacy = provider.seed("Selia · Recorded period")
        val unrelated = provider.seed("Selia · Dinner")
        val otherCalendar = provider.seed("Selia · Recorded period", calendarId = 2)
        val foreignApp = provider.seed("Selia · Recorded period", appPackage = "another.app")
        val recurring = provider.seed("Selia · Recorded period", rule = "FREQ=MONTHLY")
        val timed = provider.seed("Selia · Recorded period", allDay = 0)
        val mirror = CalendarMirror(context)
        val preview = mirror.previewCleanup(1)
        assertEquals(listOf(legacy), preview.events.map { it.id })
        assertTrue(preview.events.single().legacy)
        mirror.deletePreviewedEvents(preview, setOf(legacy))
        assertEquals(setOf(unrelated, otherCalendar, foreignApp, recurring, timed), provider.ids())
        val inserts = provider.inserts
        val restarted = CalendarMirror(context)
        restarted.snapshot(CycleBackup(listOf(DayLog(LocalDate.now(), bleeding = true, flow = Flow.LIGHT))), emptyMap())
        assertFalse(restarted.calendarSyncEnabled())
        assertEquals(inserts, provider.inserts)
    }

    @Test fun roundTripWithoutCustomFieldsKeepsProfileIdentityAndDoesNotDuplicate() = withCalendar { context, provider ->
        val backup = CycleBackup(listOf(DayLog(LocalDate.now(), bleeding = true, flow = Flow.LIGHT)))
        val mirror = CalendarMirror(context)
        mirror.connect(1, backup, emptyMap())
        val other = CalendarMirror(context, UUID.randomUUID().toString())
        other.connect(1, backup, emptyMap())
        val count = provider.ids().size
        provider.db.execSQL("UPDATE events SET customAppPackage=NULL,customAppUri=NULL,description='<p>' || description || '</p>'")
        mirror.snapshot(backup, emptyMap())
        assertEquals(count, provider.ids().size)
        mirror.disconnect()
        assertTrue(provider.ids().isNotEmpty())
        other.disconnect()
        assertTrue(provider.ids().isEmpty())
    }

    @Test fun selectionAndChangedEventGuardsProtectUnapprovedRows() = withCalendar { context, provider ->
        val first = provider.seed("Selia · Odhad menstruace")
        val second = provider.seed("Selia · Recorded period")
        val mirror = CalendarMirror(context)
        val preview = mirror.previewCleanup(1)
        assertEquals(2, preview.events.size)
        mirror.deletePreviewedEvents(preview, setOf(first))
        assertEquals(setOf(second), provider.ids())
        val stale = mirror.previewCleanup(1)
        provider.db.execSQL("UPDATE events SET title='Unrelated appointment' WHERE _id=?", arrayOf(second))
        assertThrows(CalendarCleanupException::class.java) { mirror.deletePreviewedEvents(stale, setOf(second)) }
        assertEquals(setOf(second), provider.ids())
        assertFalse(CalendarMirror(context).calendarSyncEnabled())
    }

    @Test fun ownershipMarkerKeepsNotesAddedInTheCalendar() = withCalendar { context, provider ->
        val backup = CycleBackup(listOf(DayLog(LocalDate.now(), bleeding = true, flow = Flow.LIGHT)))
        val mirror = CalendarMirror(context)
        mirror.connect(1, backup, emptyMap())
        provider.db.execSQL("UPDATE events SET description='My own calendar note' WHERE customAppUri LIKE '%/recorded/%'")
        repeat(2) { mirror.snapshot(backup, emptyMap()) }
        provider.db.rawQuery("SELECT description FROM events WHERE customAppUri LIKE '%/recorded/%'", null).use {
            assertTrue(it.moveToFirst())
            assertTrue(it.getString(0).contains("My own calendar note"))
            assertTrue(it.getString(0).contains("[SeliaCycles:"))
        }
    }

    @Test fun onlyPackageLossStillUsesTheExistingProfileUri() = withCalendar { context, provider ->
        val mirror = CalendarMirror(context)
        val backup = CycleBackup(listOf(DayLog(LocalDate.now(), bleeding = true, flow = Flow.LIGHT)))
        mirror.connect(1, backup, emptyMap())
        val before = provider.inserts
        provider.db.execSQL("UPDATE events SET customAppPackage=NULL,description=NULL")
        mirror.snapshot(backup, emptyMap())
        assertEquals(before, provider.inserts)
        mirror.disconnect()
        assertTrue(provider.ids().isEmpty())
    }

    @Test fun failedLegacyDeletionDoesNotRestartPreviouslyEnabledSync() = withCalendar { context, provider ->
        val backup = CycleBackup(listOf(DayLog(LocalDate.now(), bleeding = true, flow = Flow.LIGHT)))
        val mirror = CalendarMirror(context)
        mirror.connect(1, backup, emptyMap())
        val legacy = provider.seed("Selia · Recorded period")
        val preview = mirror.previewCleanup(1)
        provider.failDelete = true
        assertThrows(CalendarCleanupException::class.java) { mirror.deletePreviewedEvents(preview, setOf(legacy)) }
        assertTrue(legacy in provider.ids())
        val inserts = provider.inserts
        val restarted = CalendarMirror(context)
        restarted.snapshot(backup, emptyMap())
        assertFalse(restarted.calendarSyncEnabled())
        assertEquals(inserts, provider.inserts)
        provider.failDelete = false
        restarted.deletePreviewedEvents(restarted.previewCleanup(1), setOf(legacy))
        assertFalse(legacy in provider.ids())
    }

    @Test fun cleanupBatchesLegacyCopiesWithoutDeletingAnotherCalendar() = withCalendar { context, provider ->
        val ids = (1..227).mapTo(mutableSetOf()) { provider.seed("Selia · Recorded period") }
        val other = provider.seed("Selia · Recorded period", calendarId = 2)
        val mirror = CalendarMirror(context)
        val preview = mirror.previewCleanup(1)
        assertEquals(ids, preview.events.mapTo(mutableSetOf()) { it.id })
        mirror.deletePreviewedEvents(preview, ids)
        assertEquals(setOf(other), provider.ids())
        assertFalse(CalendarMirror(context).calendarSyncEnabled())
    }

    private fun withCalendar(test: (Context, Provider) -> Unit) {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        check(target.packageName.endsWith(".qa"))
        val directory = File(target.cacheDir, "legacy-calendar-${UUID.randomUUID()}").apply { mkdirs() }
        val provider = Provider()
        val context = object : ContextWrapper(target) {
            override fun getContentResolver(): ContentResolver = ContentResolver.wrap(provider)
            override fun getNoBackupFilesDir(): File = directory
            override fun checkPermission(permission: String, pid: Int, uid: Int) = PackageManager.PERMISSION_GRANTED
        }
        try { test(context, provider) } finally {
            provider.db.close()
            directory.listFiles()?.forEach { it.delete() }
            directory.delete()
        }
    }

    private class Provider : ContentProvider() {
        val db = SQLiteDatabase.create(null).apply {
            execSQL("""CREATE TABLE events (_id INTEGER PRIMARY KEY AUTOINCREMENT, calendar_id INTEGER,
                title TEXT, description TEXT, dtstart INTEGER, dtend INTEGER, eventTimezone TEXT,
                allDay INTEGER, availability INTEGER, accessLevel INTEGER, eventStatus INTEGER,
                customAppPackage TEXT, customAppUri TEXT, deleted INTEGER DEFAULT 0, rrule TEXT, rdate TEXT)""")
            execSQL("""CREATE TABLE calendars (_id INTEGER PRIMARY KEY,calendar_displayName TEXT,account_name TEXT,
                calendar_color INTEGER,calendar_access_level INTEGER DEFAULT 700,sync_events INTEGER DEFAULT 1,visible INTEGER DEFAULT 1)""")
            execSQL("INSERT INTO calendars (_id,calendar_displayName,account_name,calendar_color) VALUES (1,'QA','local',0),(2,'Other calendar','local',0)")
        }
        var inserts = 0
        var failDelete = false
        override fun onCreate() = true
        override fun getType(uri: Uri) = "vnd.android.cursor.dir/event"
        private fun where(uri: Uri, selection: String?) = listOfNotNull(
            uri.lastPathSegment?.toLongOrNull()?.let { "_id=$it" }, selection?.takeIf { it.isNotBlank() }?.let { "($it)" },
        ).joinToString(" AND ").ifEmpty { null }
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor =
            db.query(uri.pathSegments.first(), projection, where(uri, selection), selectionArgs, null, null, sortOrder)
        override fun insert(uri: Uri, values: ContentValues?): Uri {
            inserts++
            return ContentUris.withAppendedId(uri, db.insertOrThrow("events", null, values))
        }
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) =
            db.update("events", values, where(uri, selection), selectionArgs)
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            check(!failDelete) { "Simulated provider failure" }
            return db.delete("events", where(uri, selection), selectionArgs)
        }
        override fun applyBatch(operations: ArrayList<ContentProviderOperation>): Array<ContentProviderResult> {
            db.beginTransaction()
            try { return super.applyBatch(operations).also { db.setTransactionSuccessful() } }
            finally { db.endTransaction() }
        }
        fun ids(): Set<Long> = db.rawQuery("SELECT _id FROM events", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getLong(0)) }
        }
        fun seed(title: String, calendarId: Long = 1, appPackage: String? = null, rule: String? = null, allDay: Int = 1): Long =
            ContentUris.parseId(insert(Events.CONTENT_URI, ContentValues().apply {
                put(Events.CALENDAR_ID, calendarId); put(Events.TITLE, title)
                put(Events.DTSTART, 1_700_000_000_000L); put(Events.DTEND, 1_700_086_400_000L)
                put(Events.ALL_DAY, allDay); put(Events.CUSTOM_APP_PACKAGE, appPackage); put(Events.RRULE, rule)
            }))
    }
}
