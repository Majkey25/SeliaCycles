package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MyCalendarTransformTest {
    @Test
    fun `transforms periods and supported daily details`() {
        val preview = MyCalendarTransformer.transform(
            generation = "24",
            periods = listOf(MyCalendarPeriodRow(20260801, 3)),
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
        assertEquals("kept", preview.logs[1].note)
        assertEquals(Intimacy.PROTECTED, preview.logs[1].intimacy)
    }

    @Test
    fun `signed period lengths are real records and zero rows are placeholders`() {
        val preview = MyCalendarTransformer.transform(
            generation = "24",
            periods = listOf(
                MyCalendarPeriodRow(20260801, -5),
                MyCalendarPeriodRow(20260901, 0),
            ),
            notes = emptyList(),
        )

        assertEquals(5, preview.logs.size)
        assertEquals(LocalDate.of(2026, 8, 1), preview.firstDay)
        assertEquals(LocalDate.of(2026, 8, 5), preview.lastDay)
    }

    @Test
    fun `merge preserves explicit local values and adds imported fields`() {
        val day = LocalDate.of(2026, 8, 28)
        val current = DayLog(day, mood = Mood.GOOD, note = "keep", weightKg = 68.0)
        val imported = DayLog(
            day,
            bleeding = true,
            flow = Flow.MEDIUM,
            mood = Mood.BAD,
            note = "replace",
            weightKg = 70.0,
            temperatureC = 36.5,
            cervicalMucus = CervicalMucus.EGG_WHITE,
        )

        val merged = mergeDayLogs(current, imported)

        assertTrue(merged.bleeding)
        assertEquals(Flow.MEDIUM, merged.flow)
        assertEquals(Mood.GOOD, merged.mood)
        assertEquals("keep", merged.note)
        assertEquals(68.0, merged.weightKg)
        assertEquals(36.5, merged.temperatureC)
        assertEquals(CervicalMucus.EGG_WHITE, merged.cervicalMucus)
    }

    @Test
    fun `preserves unknown source codes and rejects empty backup`() {
        val preview = MyCalendarTransformer.transform(
            null,
            emptyList(),
            listOf(MyCalendarNoteRow(date = 20260828, moodCodes = "65,", symptomCodes = "24:3#")),
        )
        assertEquals(2, preview.unsupportedDetails)
        assertTrue(preview.logs.single().importedDetails.contains("mood=65,"))

        val empty = assertFailsWith<MyCalendarFormatException> {
            MyCalendarTransformer.transform(null, emptyList(), listOf(MyCalendarNoteRow(20260828)))
        }
        assertEquals(MyCalendarFailure.EMPTY, empty.failure)
    }

    @Test
    fun `rejects invalid dates period lengths and oversized notes`() {
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarTransformer.transform(null, listOf(MyCalendarPeriodRow(20260230, 5)), emptyList())
        }
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarTransformer.transform(null, listOf(MyCalendarPeriodRow(20260828, 15)), emptyList())
        }
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarTransformer.transform(
                null,
                emptyList(),
                listOf(MyCalendarNoteRow(20260828, note = "x".repeat(DayLog.MAX_NOTE_LENGTH + 1))),
            )
        }
    }
}
