package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MyCalendarTransformTest {
    @Test
    fun mergeKeepsExplicitLocalDetailsAndAddsImportedValues() {
        val day = LocalDate.of(2026, 8, 28)
        val current = DayLog(
            day = day,
            mood = Mood.GOOD,
            symptoms = setOf(Symptom.CRAMPS),
            note = "keep",
            weightKg = 68.0,
        )
        val imported = DayLog(
            day = day,
            bleeding = true,
            flow = Flow.MEDIUM,
            mood = Mood.BAD,
            symptoms = setOf(Symptom.HEADACHE),
            note = "replace",
            weightKg = 70.0,
            temperatureC = 36.5,
            intimacy = Intimacy.PROTECTED,
            importedDetails = "legacy",
        )

        assertEquals(DayLog(
            day = day,
            bleeding = true,
            flow = Flow.MEDIUM,
            mood = Mood.GOOD,
            symptoms = setOf(Symptom.CRAMPS, Symptom.HEADACHE),
            note = "keep",
            weightKg = 68.0,
            temperatureC = 36.5,
            intimacy = Intimacy.PROTECTED,
            importedDetails = "legacy",
        ), mergeDayLogs(current, imported))
    }

    @Test
    fun mergeKeepsExplicitLocalFlow() {
        val day = LocalDate.of(2026, 8, 28)

        val merged = mergeDayLogs(
            DayLog(day, bleeding = true, flow = Flow.LIGHT),
            DayLog(day, bleeding = true, flow = Flow.HEAVY),
        )

        assertEquals(Flow.LIGHT, merged.flow)
    }

    @Test
    fun rejectsMeasurementsOutsideSafeStorageBounds() {
        val day = LocalDate.of(2026, 8, 28)

        assertFailsWith<IllegalArgumentException> { DayLog(day, weightKg = 10.0) }
        assertFailsWith<IllegalArgumentException> { DayLog(day, temperatureC = 50.0) }
        assertFailsWith<IllegalArgumentException> { DayLog(day, sleepHours = 25.0) }
    }

    @Test
    fun transformsPeriodsAndSupportedDailyDetails() {
        val preview = MyCalendarTransformer.transform(
            generation = "24",
            periods = listOf(MyCalendarPeriodRow(date = 20260801, periodLength = 3)),
            notes = listOf(MyCalendarNoteRow(
                date = 20260802,
                weightKg = 68.4,
                temperatureC = 36.6,
                sleep = "7.5",
                note = "kept",
                intimate = 1,
                condom = 1,
            )),
        )

        assertEquals(3, preview.logs.size)
        assertEquals(LocalDate.of(2026, 8, 1), preview.firstDay)
        assertEquals(LocalDate.of(2026, 8, 3), preview.lastDay)
        assertEquals("24", preview.generation)
        assertEquals(DayLog(
            day = LocalDate.of(2026, 8, 2),
            bleeding = true,
            flow = Flow.UNKNOWN,
            note = "kept",
            weightKg = 68.4,
            temperatureC = 36.6,
            sleepHours = 7.5,
            intimacy = Intimacy.PROTECTED,
        ), preview.logs[1])
    }

    @Test
    fun preservesUnknownSourceCodesWithoutInventingLabels() {
        val preview = MyCalendarTransformer.transform(
            generation = null,
            periods = emptyList(),
            notes = listOf(MyCalendarNoteRow(
                date = 20260828,
                moodCodes = "65,",
                symptomCodes = "24:3#",
                cervicalFluid = "legacy",
                pregnancyTest = 2,
            )),
        )

        assertEquals(4, preview.unsupportedDetails)
        assertTrue(preview.logs.single().importedDetails.contains("mood=65,"))
        assertTrue(preview.logs.single().importedDetails.contains("symptoms=24:3#"))
    }

    @Test
    fun ignoresInvalidLegacyMeasurementsButReportsThem() {
        val preview = MyCalendarTransformer.transform(
            generation = null,
            periods = emptyList(),
            notes = listOf(MyCalendarNoteRow(date = 20260828, weightKg = 10.0, temperatureC = 50.0)),
        )

        assertEquals(2, preview.unsupportedDetails)
        assertEquals(null, preview.logs.single().weightKg)
        assertEquals(null, preview.logs.single().temperatureC)
    }

    @Test
    fun rejectsInvalidDatesAndEmptyBackups() {
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarTransformer.transform(null, listOf(MyCalendarPeriodRow(20260230, 5)), emptyList())
        }
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarTransformer.transform(null, listOf(MyCalendarPeriodRow(20260828, 15)), emptyList())
        }
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarTransformer.transform(null, emptyList(), listOf(MyCalendarNoteRow(20260828)))
        }
    }

    @Test
    fun rejectsOversizedImportedNote() {
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarTransformer.transform(
                null,
                emptyList(),
                listOf(MyCalendarNoteRow(date = 20260828, note = "x".repeat(DayLog.MAX_NOTE_LENGTH + 1))),
            )
        }
    }
}
