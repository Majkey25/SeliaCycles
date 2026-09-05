package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonalPatternsTest {
    private val first = LocalDate.of(2026, 1, 1)
    private val second = LocalDate.of(2026, 1, 29)
    private val third = LocalDate.of(2026, 2, 26)

    @Test
    fun `impossible cycle phase settings preserve observed menstrual patterns only`() {
        val starts = listOf(first, first.plusDays(15), first.plusDays(30))
        val backup = CycleBackup(
            logs = trackedPeriods(starts, mapOf(
                first.plusDays(1) to setOf(Symptom.CRAMPS),
                first.plusDays(2) to setOf(Symptom.CRAMPS),
                first.plusDays(16) to setOf(Symptom.CRAMPS),
            )) + listOf(6L, 7L, 21L).map { DayLog(first.plusDays(it), symptoms = setOf(Symptom.HEADACHE)) },
            settings = AppSettings(lutealPhaseLength = 19),
        )

        assertEquals(listOf(Symptom.CRAMPS), PersonalPatterns.symptomPatterns(backup).map(SymptomPattern::symptom))
    }

    @Test
    fun `finds a recurring symptom phase across completed cycles`() {
        val backup = CycleBackup(logs = trackedPeriods(listOf(first, second, third), mapOf(
            first.plusDays(1) to setOf(Symptom.CRAMPS),
            first.plusDays(2) to setOf(Symptom.CRAMPS),
            second.plusDays(1) to setOf(Symptom.CRAMPS),
        )))

        assertEquals(
            SymptomPattern(Symptom.CRAMPS, CyclePhase.MENSTRUAL, sampleCount = 3, cycleCount = 2),
            PersonalPatterns.symptomPatterns(backup).single(),
        )
    }

    @Test
    fun `requires three samples across two completed cycles`() {
        val twoSamples = CycleBackup(logs = trackedPeriods(listOf(first, second, third), mapOf(
            first.plusDays(1) to setOf(Symptom.HEADACHE),
            second.plusDays(1) to setOf(Symptom.HEADACHE),
        )))
        val oneCycle = CycleBackup(logs = trackedPeriods(listOf(first, second), mapOf(
            first.plusDays(1) to setOf(Symptom.BLOATING),
            first.plusDays(2) to setOf(Symptom.BLOATING),
            first.plusDays(3) to setOf(Symptom.BLOATING),
        )))

        assertTrue(PersonalPatterns.symptomPatterns(twoSamples).isEmpty())
        assertTrue(PersonalPatterns.symptomPatterns(oneCycle).isEmpty())
    }

    @Test
    fun `counts each symptom once per day and ranks the strongest pattern first`() {
        val backup = CycleBackup(logs = trackedPeriods(listOf(first, second, third), mapOf(
            first.plusDays(1) to setOf(Symptom.FATIGUE, Symptom.CRAVINGS),
            first.plusDays(2) to setOf(Symptom.FATIGUE),
            second.plusDays(1) to setOf(Symptom.FATIGUE, Symptom.CRAVINGS),
            second.plusDays(2) to setOf(Symptom.FATIGUE, Symptom.CRAVINGS),
        )))

        val patterns = PersonalPatterns.symptomPatterns(backup)

        assertEquals(Symptom.FATIGUE, patterns.first().symptom)
        assertEquals(4, patterns.first().sampleCount)
        assertEquals(3, patterns.single { it.symptom == Symptom.CRAVINGS }.sampleCount)
    }

    @Test
    fun `classifies repeated premenstrual symptoms as luteal`() {
        val backup = CycleBackup(logs = trackedPeriods(listOf(first, second, third), emptyMap()) + listOf(
            DayLog(first.plusDays(19), symptoms = setOf(Symptom.BACKACHE)),
            DayLog(first.plusDays(20), symptoms = setOf(Symptom.BACKACHE)),
            DayLog(second.plusDays(20), symptoms = setOf(Symptom.BACKACHE)),
        ))

        assertEquals(CyclePhase.LUTEAL, PersonalPatterns.symptomPatterns(backup).single().phase)
    }

    private fun period(start: LocalDate): List<DayLog> = (0L..4L).map { offset ->
        DayLog(start.plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
    }

    private fun trackedPeriods(
        starts: List<LocalDate>,
        symptoms: Map<LocalDate, Set<Symptom>>,
    ): List<DayLog> = starts.flatMap(::period).map { log -> log.copy(symptoms = symptoms[log.day].orEmpty()) }
}
