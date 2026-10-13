package com.majkeylab.seliacycles

import android.database.sqlite.SQLiteConstraintException
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class RestoreSnapshotTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun restoreReplacesOnlyReconstructedSnapshotsAndKeepsRemindersDisabled() = withStore { store ->
        val month = YearMonth.now().minusMonths(4)
        val reconstructed = snapshot(month, 10, true)
        val localOriginal = snapshot(month.plusMonths(1), 10, false)
        val conflictingOriginal = snapshot(month.plusMonths(2), 10, false)
        store.saveForecastSnapshots(listOf(reconstructed, localOriginal, conflictingOriginal))
        val restoredOriginal = snapshot(month, 12, false)
        val addedOriginal = snapshot(month.plusMonths(3), 12, false)
        val backup = CycleBackup(
            listOf(DayLog(LocalDate.now().minusDays(1), note = "QA imported record")),
            AppSettings(cycleLength = 31, reminderEnabled = true, contraceptionReminderEnabled = true),
        )

        store.mergeTransfer(SeliaTransfer(backup, listOf(
            restoredOriginal,
            snapshot(localOriginal.month, 12, true),
            snapshot(conflictingOriginal.month, 12, false),
            addedOriginal,
        )))

        assertEquals(listOf(restoredOriginal, localOriginal, conflictingOriginal, addedOriginal), store.loadForecastSnapshots())
        assertEquals(backup.logs, store.load().logs)
        assertEquals(31, store.load().settings.cycleLength)
        assertFalse(store.load().settings.reminderEnabled)
        assertFalse(store.load().settings.contraceptionReminderEnabled)
    }

    @Test fun failedSnapshotRestoreRollsBackLogsSettingsAndDeletedSnapshot() = withStore { store ->
        val day = LocalDate.now().minusDays(2)
        val before = CycleBackup(listOf(DayLog(day, note = "QA local record")), AppSettings(cycleLength = 27))
        val reconstructed = snapshot(YearMonth.from(day).minusMonths(1), 10, true)
        store.replace(before)
        store.saveForecastSnapshots(listOf(reconstructed))
        store.writableDatabase.execSQL(
            "CREATE TRIGGER qa_reject_snapshot BEFORE INSERT ON forecast_snapshots BEGIN SELECT RAISE(ABORT, 'QA snapshot restore failure'); END",
        )
        val incoming = SeliaTransfer(
            CycleBackup(listOf(DayLog(day.plusDays(1), note = "QA incoming record")), AppSettings(cycleLength = 31)),
            listOf(reconstructed.copy(reconstructed = false)),
        )

        assertThrows(SQLiteConstraintException::class.java) { store.mergeTransfer(incoming) }

        assertEquals(before, store.load())
        assertEquals(listOf(reconstructed), store.loadForecastSnapshots())
    }

    private fun snapshot(month: YearMonth, day: Int, reconstructed: Boolean): ForecastSnapshot {
        val start = month.atDay(day)
        return ForecastSnapshot(month, start, start.minusDays(2), start.plusDays(2), 5, reconstructed)
    }

    private fun withStore(block: (CycleStore) -> Unit) {
        check(context.packageName.endsWith(".qa"))
        val id = UUID.randomUUID().toString()
        try {
            CycleStore(context, id).use(block)
        } finally {
            context.deleteDatabase(profileDatabaseName(id))
        }
    }
}
