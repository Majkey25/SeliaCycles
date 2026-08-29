package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class DayLogTrackersTest {
    private val day = LocalDate.of(2026, 8, 29)

    @Test
    fun `spotting is saved without becoming period bleeding`() {
        val log = DayLog(day = day, spotting = true)

        assertFalse(log.bleeding)
        assertFalse(log.isEmpty)
        assertNull(AppState(backup = CycleBackup(logs = listOf(log))).prediction.nextPeriodStart)
    }

    @Test
    fun `pain accepts inclusive zero to ten range`() {
        assertFalse(DayLog(day, painLevel = 0).isEmpty)
        assertFalse(DayLog(day, painLevel = 10).isEmpty)
        assertFailsWith<IllegalArgumentException> { DayLog(day, painLevel = -1) }
        assertFailsWith<IllegalArgumentException> { DayLog(day, painLevel = 11) }
    }

    @Test
    fun `each fertility and wellbeing tracker makes a record non empty`() {
        val logs = listOf(
            DayLog(day, cervicalMucus = CervicalMucus.EGG_WHITE),
            DayLog(day, ovulationTest = TestResult.POSITIVE),
            DayLog(day, pregnancyTest = TestResult.NEGATIVE),
            DayLog(day, energy = WellbeingLevel.HIGH),
            DayLog(day, stress = WellbeingLevel.LOW),
            DayLog(day, activity = ActivityLevel.MODERATE),
            DayLog(day, medication = MedicationStatus.TAKEN),
        )

        assertTrue(logs.none(DayLog::isEmpty))
    }
}
