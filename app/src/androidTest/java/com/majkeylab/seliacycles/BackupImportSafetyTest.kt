package com.majkeylab.seliacycles

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupImportSafetyTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun requireIsolatedPackage() {
        check(context.packageName.endsWith(".qa"))
    }

    @Test fun exportedPayloadLargerThanDefaultCursorWindowRoundTrips() {
        val firstDay = LocalDate.of(2010, 1, 1)
        val note = "x".repeat(1_000)
        val transfer = SeliaTransfer(CycleBackup(List(2_500) { index ->
            DayLog(firstDay.plusDays(index.toLong()), note = note)
        }), emptyList())
        val payloadSize = SeliaBackupCodec.encode(transfer).size
        assertTrue(payloadSize > 2 * 1024 * 1024)
        assertTrue(payloadSize <= SeliaBackupCodec.MAX_BYTES)

        val output = ByteArrayOutputStream()
        MyCalendarExporter(context).write(transfer, output)
        val restored = MyCalendarImporter(context).inspect(ByteArrayInputStream(output.toByteArray()))

        assertEquals(transfer, restored.seliaTransfer)
    }

    @Test fun ordinaryTablesImportButMatchingViewsAreRejected() {
        val importer = MyCalendarImporter(context)
        val ordinary = importer.inspect(ByteArrayInputStream(databaseBackup { }))
        assertEquals(listOf(DayLog(LocalDate.of(2010, 1, 1), bleeding = true, flow = Flow.UNKNOWN, note = "synthetic")), ordinary.logs)

        for (table in listOf("Period", "Note", "SeliaBackup")) {
            val backup = databaseBackup { database ->
                if (table == "SeliaBackup") {
                    database.execSQL("CREATE VIEW SeliaBackup AS SELECT 1 AS version, X'00' AS payload")
                } else {
                    database.execSQL("ALTER TABLE $table RENAME TO original_$table")
                    database.execSQL("CREATE VIEW $table AS SELECT * FROM original_$table")
                }
            }
            val error = assertThrows(MyCalendarFormatException::class.java) {
                importer.inspect(ByteArrayInputStream(backup))
            }
            assertEquals(MyCalendarFailure.UNSUPPORTED, error.failure)
        }
    }

    @Test fun largerCursorWindowDoesNotRaiseThePayloadLimit() {
        val backup = databaseBackup { database ->
            database.execSQL("CREATE TABLE SeliaBackup (version INTEGER, payload BLOB)")
            database.execSQL("INSERT INTO SeliaBackup VALUES (1, ?)", arrayOf(ByteArray(SeliaBackupCodec.MAX_BYTES + 1)))
        }

        val error = assertThrows(MyCalendarFormatException::class.java) {
            MyCalendarImporter(context).inspect(ByteArrayInputStream(backup))
        }

        assertEquals(MyCalendarFailure.DAMAGED, error.failure)
        assertEquals("Selia backup is too large", error.message)
    }

    @Test fun duplicateSeliaPayloadRowsAreStillRejected() {
        val payload = SeliaBackupCodec.encode(SeliaTransfer(
            CycleBackup(listOf(DayLog(LocalDate.of(2010, 1, 1), note = "synthetic"))), emptyList(),
        ))
        val backup = databaseBackup { database ->
            database.execSQL("CREATE TABLE SeliaBackup (version INTEGER, payload BLOB)")
            repeat(2) { database.execSQL("INSERT INTO SeliaBackup VALUES (1, ?)", arrayOf(payload)) }
        }

        val error = assertThrows(MyCalendarFormatException::class.java) {
            MyCalendarImporter(context).inspect(ByteArrayInputStream(backup))
        }

        assertEquals(MyCalendarFailure.UNSUPPORTED, error.failure)
    }

    private fun databaseBackup(change: (SQLiteDatabase) -> Unit): ByteArray {
        val file = File.createTempFile("backup-import-safety-", ".db", context.cacheDir)
        return try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
                database.execSQL("CREATE TABLE Period (date INTEGER, period INTEGER)")
                database.execSQL("INSERT INTO Period VALUES (20100101, 1)")
                database.execSQL("""CREATE TABLE Note (
                    date INTEGER, weight REAL, temperature REAL, sleep TEXT, note TEXT,
                    intimate INTEGER, condom INTEGER, mood TEXT, symptom TEXT, cervical_fluid TEXT,
                    pregnancy_test INTEGER, ovulation_test INTEGER, fertility_test INTEGER
                )""")
                database.execSQL("INSERT INTO Note (date, note) VALUES (20100101, 'synthetic')")
                change(database)
            }
            val output = ByteArrayOutputStream()
            MyCalendarContainerWriter.write(file.readBytes(), output)
            output.toByteArray()
        } finally {
            SQLiteDatabase.deleteDatabase(file)
        }
    }
}
