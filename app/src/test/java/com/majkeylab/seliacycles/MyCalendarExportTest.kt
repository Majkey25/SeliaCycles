package com.majkeylab.seliacycles

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MyCalendarExportTest {
    @Test
    fun `maximum supported history fits the backup entry limit`() {
        val logs = List(CycleBackup.MAX_LOGS) { index ->
            DayLog(DayLog.MIN_DATE.plusDays(index.toLong()), note = "note")
        }

        val chunks = MyCalendarExportMapper.noteChunks(logs)

        assertTrue(chunks.size <= MyCalendarExportMapper.MAX_NOTE_FILES)
        assertEquals(CycleBackup.MAX_LOGS, chunks.sumOf(List<MyCalendarNoteRow>::size))
    }

    @Test
    fun `container writer matches the generation 7 envelope`() {
        val database = "SQLite format 3\u0000test".encodeToByteArray()
        val output = ByteArrayOutputStream()

        MyCalendarContainerWriter.write(database, output)

        val container = MyCalendarContainerReader.read(ByteArrayInputStream(output.toByteArray()))
        assertContentEquals(database, container.database)
        assertEquals("7", container.generation)
    }

    @Test
    fun `container writer refuses an unreadable entry count`() {
        val database = "SQLite format 3\u0000test".encodeToByteArray()
        val sidecars = (1..29).associate { index -> "$index.extra" to byteArrayOf() }

        assertFailsWith<IllegalArgumentException> {
            MyCalendarContainerWriter.write(database, ByteArrayOutputStream(), sidecars)
        }
    }

    @Test
    fun `My Calendar sidecar encoding is symmetric`() {
        val json = """[{"period":5,"date_str":"2026-08-31"}]"""
        val encoded = encryptedMyCalendarJson(json)

        assertEquals(json, encryptedMyCalendarJson(encoded.decodeToString()).decodeToString())
    }

    @Test
    fun `Selia payload preserves every local field`() {
        val day = LocalDate.of(2026, 8, 31)
        val transfer = SeliaTransfer(
            backup = CycleBackup(
                logs = listOf(DayLog(
                    day = day,
                    bleeding = true,
                    spotting = true,
                    flow = Flow.HEAVY,
                    mood = Mood.GOOD,
                    symptoms = setOf(Symptom.CRAMPS, Symptom.BACKACHE),
                    note = "private",
                    weightKg = 68.5,
                    temperatureC = 36.7,
                    sleepHours = 7.5,
                    intimacy = Intimacy.PROTECTED,
                    cervicalMucus = CervicalMucus.EGG_WHITE,
                    ovulationTest = TestResult.POSITIVE,
                    pregnancyTest = TestResult.NEGATIVE,
                    painLevel = 7,
                    energy = WellbeingLevel.LOW,
                    stress = WellbeingLevel.HIGH,
                    activity = ActivityLevel.MODERATE,
                    medication = MedicationStatus.TAKEN,
                    importedDetails = "source=kept",
                )),
                settings = AppSettings(
                    cycleLength = 31,
                    periodLength = 6,
                    cycleLengthOverride = 32,
                    periodLengthOverride = 6,
                    activePeriodStart = day,
                    theme = AppTheme.DARK,
                    palette = AppPalette.CUSTOM,
                    customPalette = CustomPalette(1, 2, 3, 4),
                    profile = UserProfile(20, 172, 50.0, TrackingGoal.TRYING_TO_CONCEIVE),
                ),
            ),
            snapshots = listOf(ForecastSnapshot(
                month = YearMonth.of(2026, 8),
                periodStart = day,
                earliestStart = day.minusDays(2),
                latestStart = day.plusDays(2),
                periodLength = 6,
                reconstructed = false,
            )),
        )

        val decoded = SeliaBackupCodec.decode(SeliaBackupCodec.encode(transfer))

        assertEquals(transfer, decoded)
    }

    @Test
    fun `export mapper emits completed period runs and common notes`() {
        val start = LocalDate.of(2026, 8, 1)
        val logs = listOf(
            DayLog(start, bleeding = true, flow = Flow.MEDIUM),
            DayLog(start.plusDays(1), bleeding = true, flow = Flow.HEAVY, note = "kept", weightKg = 60.0),
            DayLog(start.plusDays(30), bleeding = true, flow = Flow.LIGHT),
        )

        assertEquals(
            listOf(MyCalendarPeriodRow(20260801, 2), MyCalendarPeriodRow(20260831, 1)),
            MyCalendarExportMapper.periodRows(logs),
        )
        assertEquals("kept", MyCalendarExportMapper.noteRows(logs).single().note)
    }
}
