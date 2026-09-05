package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonthlySummaryTest {
    @Test
    fun `summary counts only actual month days and preserves separate runs`() {
        val month = YearMonth.of(2026, 9)
        val logs = listOf(31L, 32L, 33L, 35L, 61L).map { offset ->
            DayLog(LocalDate.of(2026, 7, 31).plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
        } + DayLog(month.atDay(8), mood = Mood.GOOD, painLevel = 3, sleepHours = 8.0) +
            DayLog(month.atDay(9), energy = WellbeingLevel.HIGH, sleepHours = 6.0) +
            DayLog(month.atDay(25), mood = Mood.BAD)

        val summary = MonthlySummary.create(month, logs, emptyList(), emptyList(), month.atDay(20))

        assertEquals(setOf(month.atDay(1), month.atDay(2), month.atDay(4)), summary.recordedDays)
        assertEquals(listOf(month.atDay(1)..month.atDay(2), month.atDay(4)..month.atDay(4)), summary.recordedRuns)
        assertEquals(3, summary.detailDays.size)
        assertEquals(mapOf(Mood.GOOD to 1), summary.moodCounts)
        assertEquals(mapOf(WellbeingLevel.HIGH to 1), summary.energyCounts)
        assertEquals(1, summary.painDays)
        assertEquals(7.0, summary.sleepAverage)
    }

    @Test
    fun `month includes every overlapping estimate including previous month`() {
        val month = YearMonth.of(2026, 9)
        val starts = listOf(LocalDate.of(2026, 8, 30), month.atDay(26), LocalDate.of(2026, 10, 24))
        val estimates = starts.map { PeriodEstimate(it, it.plusDays(5), it.minusDays(2), it.plusDays(2), EstimateOrigin.CURRENT) }
        val fertility = starts.map { CycleInsights.fertilityForPeriod(it) }
        val summary = MonthlySummary.create(month, emptyList(), estimates, fertility, month.atDay(5))

        assertEquals(starts.take(2), summary.estimates.map(PeriodEstimate::start))
        assertEquals(listOf(month.atDay(26)), summary.fertility.map(FertilityEstimate::periodStart))
        assertTrue(summary.recordedDays.isEmpty())
        assertTrue(summary.detailDays.isEmpty())
    }
}
