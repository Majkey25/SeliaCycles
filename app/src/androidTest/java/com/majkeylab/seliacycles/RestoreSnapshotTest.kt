package com.majkeylab.seliacycles

import android.database.sqlite.SQLiteConstraintException
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
        )), restoreSettings = true)

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

        assertThrows(SQLiteConstraintException::class.java) { store.mergeTransfer(incoming, restoreSettings = true) }

        assertEquals(before, store.load())
        assertEquals(listOf(reconstructed), store.loadForecastSnapshots())
    }

    @Test fun savingAnExistingSnapshotPreservesItAndStillAddsOtherMonths() = withStore { store ->
        val month = YearMonth.now().minusMonths(4)
        val original = snapshot(month, 10, false)
        val added = snapshot(month.plusMonths(1), 12, false)
        store.saveForecastSnapshots(listOf(original))

        store.saveForecastSnapshots(listOf(snapshot(month, 15, true), added))

        assertEquals(listOf(original, added), store.loadForecastSnapshots())
    }

    @Test fun importKeepsLocalSettingsWithoutRestoreConsent() = withStore { store ->
        val current = AppSettings(cycleLength = 27, predictionsEnabled = false)
        store.replace(CycleBackup(settings = current))
        val incoming = CycleBackup(listOf(DayLog(LocalDate.now().minusDays(1), note = "Imported record")),
            AppSettings(cycleLength = 31, partnerViewEnabled = true, reminderEnabled = true))

        store.mergeTransfer(SeliaTransfer(incoming, emptyList()), restoreSettings = false)

        assertEquals(current, store.load().settings)
        assertEquals(incoming.logs, store.load().logs)
    }

    @Test fun readingDuringPeriodWritesKeepsLogsAndSettingsFromTheSameSnapshot() = withStore { store ->
        val first = LocalDate.of(2020, 1, 1)
        val second = first.plusDays(28)
        fun write(day: LocalDate) = store.savePeriodState(
            listOf(DayLog(day, bleeding = true, flow = Flow.UNKNOWN)),
            AppSettings(activePeriodStart = day),
        )
        write(first)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val writer = pool.submit {
                start.await()
                repeat(200) { write(if (it % 2 == 0) second else first) }
            }
            val reader = pool.submit {
                start.await()
                repeat(400) {
                    val backup = store.load()
                    assertEquals(backup.logs.single().day, backup.settings.activePeriodStart)
                }
            }
            start.countDown()
            writer.get(15, TimeUnit.SECONDS)
            reader.get(15, TimeUnit.SECONDS)
        } finally {
            pool.shutdown()
            assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS))
        }
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
