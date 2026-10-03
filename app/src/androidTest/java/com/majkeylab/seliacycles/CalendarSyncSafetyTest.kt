package com.majkeylab.seliacycles

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class CalendarSyncSafetyTest {
    @Test fun deletionFailureCannotRestartSync() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        check(target.packageName.endsWith(".qa"))
        val provider = TestCalendarProvider()
        val directory = File(target.cacheDir, "calendar-test-${UUID.randomUUID()}").apply { mkdirs() }
        var granted = true
        val context = object : ContextWrapper(target) {
            override fun getContentResolver(): ContentResolver = ContentResolver.wrap(provider)
            override fun getNoBackupFilesDir(): File = directory
            override fun checkSelfPermission(permission: String): Int = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int = checkSelfPermission(permission)
        }
        try {
            val backup = CycleBackup(listOf(DayLog(LocalDate.now(), bleeding = true, flow = Flow.LIGHT)))
            CalendarMirror(context).connect(1, backup, emptyMap())
            assertTrue(provider.inserts > 0)
            provider.failDelete = true
            assertThrows(Exception::class.java) { CalendarMirror(context).disconnect() }
            // Model the provider losing old rows while the app is closed.
            provider.rows.clear()
            provider.failDelete = false
            val before = provider.inserts
            CalendarMirror(context).snapshot(backup, emptyMap())
            CalendarMirror(context).snapshot(backup, emptyMap())
            assertEquals("OFF must survive provider errors and new instances", before, provider.inserts)
            assertFalse(CalendarMirror(context).calendarSyncEnabled())
            assertFalse(CalendarMirror(context).snapshot(backup, emptyMap()).calendarSyncEnabled)
            // Only an explicit reconnection may resume writing.
            CalendarMirror(context).connect(1, backup, emptyMap())
            assertTrue(provider.inserts > before)
            granted = false
            assertThrows(CalendarCleanupException::class.java) { CalendarMirror(context).disconnect() }
            assertFalse(CalendarMirror(context).calendarSyncEnabled())
            provider.rows.clear()
            granted = true
            val afterPermissionLoss = provider.inserts
            CalendarMirror(context).snapshot(backup, emptyMap())
            assertEquals(afterPermissionLoss, provider.inserts)
            // Cleanup is scoped to this profile, not the whole calendar or another profile.
            CalendarMirror(context).connect(1, backup, emptyMap())
            val other = CalendarMirror(context, UUID.randomUUID().toString())
            other.connect(1, backup, emptyMap())
            val otherIds = provider.rows.filterValues {
                it.getAsString(CalendarContract.Events.CUSTOM_APP_URI).startsWith("selia://profile-calendar-mirror/")
            }.keys.toSet()
            CalendarMirror(context).disconnect()
            assertEquals(otherIds, provider.rows.keys)
            val afterCleanup = provider.inserts
            CalendarMirror(context).snapshot(backup, emptyMap())
            assertEquals(afterCleanup, provider.inserts)
        } finally {
            directory.listFiles()?.forEach { it.delete() }
            directory.delete()
        }
    }

    private class TestCalendarProvider : ContentProvider() {
        val rows = linkedMapOf<Long, ContentValues>()
        var inserts = 0
        var failDelete = false
        override fun onCreate() = true
        override fun getType(uri: Uri) = "vnd.android.cursor.dir/event"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            val columns = requireNotNull(projection)
            return MatrixCursor(columns).apply {
                if (uri == CalendarContract.Calendars.CONTENT_URI) {
                    addRow(arrayOf<Any>(1L, "QA only", "local", 0))
                } else rows.forEach { (id, values) ->
                    if (values.getAsString(CalendarContract.Events.CUSTOM_APP_PACKAGE) == selectionArgs?.get(0) &&
                        values.getAsString(CalendarContract.Events.CUSTOM_APP_URI).startsWith(selectionArgs[1].removeSuffix("%"))) {
                        addRow(columns.map { if (it == "_id") id else values.get(it) }.toTypedArray())
                    }
                }
            }
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri {
            val id = (++inserts).toLong()
            rows[id] = ContentValues(requireNotNull(values))
            return ContentUris.withAppendedId(uri, id)
        }
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            rows[ContentUris.parseId(uri)]?.putAll(requireNotNull(values)) ?: return 0
            return 1
        }
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            if (failDelete) throw SecurityException("Simulated provider deletion failure")
            return if (rows.remove(ContentUris.parseId(uri)) != null) 1 else 0
        }
    }
}
