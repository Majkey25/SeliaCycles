package com.majkeylab.seliacycles

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomaticBleedingStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migrationFromVersionTenPreservesRecordsAndDefaultsToConfirmed() = checkMigration(false)
    @Test fun migrationFillsAnExistingOpenSingleDayPeriod() = checkMigration(true)

    private fun checkMigration(open: Boolean) {
        check(context.packageName.endsWith(".qa"))
        val id = UUID.randomUUID().toString()
        val name = profileDatabaseName(id)
        val original = DayLog(LocalDate.now(), bleeding = true, flow = Flow.MEDIUM, note = "QA preserve")
        try {
            CycleStore(context, id).use {
                it.saveLog(original)
                if (open) it.saveSettings(AppSettings(activePeriodStart = original.day, periodLengthOverride = 6))
            }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("ALTER TABLE day_logs RENAME TO newer_logs")
                db.execSQL("""CREATE TABLE day_logs (
                    day INTEGER PRIMARY KEY, bleeding INTEGER NOT NULL, flow TEXT NOT NULL, mood TEXT,
                    symptoms TEXT NOT NULL, note TEXT NOT NULL, weight_kg REAL, temperature_c REAL,
                    sleep_hours REAL, intimacy TEXT, imported_details TEXT NOT NULL, spotting INTEGER NOT NULL,
                    cervical_mucus TEXT, ovulation_test TEXT, pregnancy_test TEXT, pain_level INTEGER,
                    energy TEXT, stress TEXT, activity TEXT, medication TEXT)""")
                db.execSQL("""INSERT INTO day_logs SELECT day,bleeding,flow,mood,symptoms,note,weight_kg,
                    temperature_c,sleep_hours,intimacy,imported_details,spotting,cervical_mucus,
                    ovulation_test,pregnancy_test,pain_level,energy,stress,activity,medication FROM newer_logs""")
                db.execSQL("DROP TABLE newer_logs")
                db.version = 10
            }
            CycleStore(context, id).use {
                assertEquals(listOf(original), it.load().logs.filterNot(DayLog::automaticBleeding))
                assertEquals(if (open) 6 else 1, it.load().logs.size)
                assertEquals(11, it.readableDatabase.version)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun fullRangeSurvivesDatabaseReopenAndPcExportImport() {
        check(context.packageName.endsWith(".qa"))
        val id = UUID.randomUUID().toString()
        val today = LocalDate.now()
        val logs = PeriodActions.start(today, emptyList(), 6)
        val backup = CycleBackup(logs, AppSettings(activePeriodStart = today))
        try {
            CycleStore(context, id).use { it.replace(backup) }
            val saved = CycleStore(context, id).use { it.load() }
            assertEquals(backup, saved)
            val bytes = ByteArrayOutputStream()
            MyCalendarExporter(context).write(SeliaTransfer(saved, emptyList()), bytes)
            val restored = MyCalendarImporter(context).inspect(ByteArrayInputStream(bytes.toByteArray()))
            assertEquals(backup, restored.seliaTransfer?.backup)
        } finally { context.deleteDatabase(profileDatabaseName(id)) }
    }
}
