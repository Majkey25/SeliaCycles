package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
