package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CycleInsightsTest {
    @Test
    fun derivesEstimatedOvulationAndFertileWindowFromPeriodStart() {
        val fertility = CycleInsights.fertilityForPeriod(LocalDate.of(2026, 8, 29))

        assertEquals(LocalDate.of(2026, 8, 15), fertility.ovulation)
        assertEquals(LocalDate.of(2026, 8, 10), fertility.fertileStart)
        assertEquals(LocalDate.of(2026, 8, 16), fertility.fertileEnd)
    }

    @Test
    fun predictsMoodOnlyFromEnoughPersonalSamePhaseHistory() {
        val logs = period(LocalDate.of(2026, 6, 1), Mood.GOOD, Mood.GOOD) +
            period(LocalDate.of(2026, 7, 1), Mood.GOOD) +
            period(LocalDate.of(2026, 8, 1))

        val insight = CycleInsights.forDate(CycleBackup(logs = logs), emptyMap(), LocalDate.of(2026, 8, 1))

        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
        assertEquals(PersonalMoodTrend(Mood.GOOD, sampleCount = 3, cycleCount = 2), insight.moodTrend)
    }

    @Test
    fun omitsMoodWhenEvidenceIsInsufficientOrSplit() {
        val insufficient = period(LocalDate.of(2026, 6, 1), Mood.GOOD, Mood.GOOD) +
            period(LocalDate.of(2026, 7, 1))
        val split = period(LocalDate.of(2026, 5, 1), Mood.GOOD, Mood.BAD) +
            period(LocalDate.of(2026, 6, 1), Mood.GOOD, Mood.BAD) +
            period(LocalDate.of(2026, 7, 1))

        assertNull(CycleInsights.forDate(CycleBackup(logs = insufficient), emptyMap(), LocalDate.of(2026, 7, 1)).moodTrend)
        assertNull(CycleInsights.forDate(CycleBackup(logs = split), emptyMap(), LocalDate.of(2026, 7, 1)).moodTrend)
    }

    @Test
    fun treatsAnEstimatedPeriodDayAsMenstrualPhase() {
        val logs = period(LocalDate.of(2026, 6, 1)) + period(LocalDate.of(2026, 7, 1))

        val insight = CycleInsights.forDate(CycleBackup(logs = logs), emptyMap(), LocalDate.of(2026, 7, 31))

        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
    }

    @Test
    fun keepsASecondFuturePeriodInTheSameMonthAfterRealityIsLogged() {
        val month = java.time.YearMonth.of(2026, 8)
        val snapshot = ForecastSnapshot(
            month = month,
            periodStart = LocalDate.of(2026, 8, 5),
            earliestStart = LocalDate.of(2026, 8, 3),
            latestStart = LocalDate.of(2026, 8, 7),
            periodLength = 3,
            reconstructed = false,
        )
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 8, 10)),
            settings = AppSettings(cycleLength = 15, periodLength = 3),
        )

        val starts = CycleInsights.periodEstimates(backup, mapOf(month to snapshot), LocalDate.of(2026, 8, 20))
            .map(PeriodEstimate::start)

        assertEquals(listOf(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 25)), starts.take(2))
    }

    private fun period(start: LocalDate, vararg moods: Mood): List<DayLog> = (0L..2L).map { offset ->
        DayLog(
            day = start.plusDays(offset),
            bleeding = true,
            flow = Flow.UNKNOWN,
            mood = moods.getOrNull(offset.toInt()),
        )
    }
}
