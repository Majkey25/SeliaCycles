package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DayOverviewTest {
    private val month = YearMonth.of(2026, 8)
    private val estimate = ForecastSnapshot(
        month = month,
        periodStart = LocalDate.of(2026, 8, 10),
        earliestStart = LocalDate.of(2026, 8, 8),
        latestStart = LocalDate.of(2026, 8, 12),
        periodLength = 5,
        reconstructed = false,
    )

    @Test
    fun `comparison reports exact early and late reality`() {
        assertEquals(EstimateAccuracy.EXACT, compare(LocalDate.of(2026, 8, 10)).accuracy)
        assertEquals(EstimateAccuracy.EARLY, compare(LocalDate.of(2026, 8, 8)).accuracy)
        assertEquals(EstimateAccuracy.LATE, compare(LocalDate.of(2026, 8, 13)).accuracy)
        assertEquals(-2, compare(LocalDate.of(2026, 8, 8)).differenceDays)
        assertEquals(3, compare(LocalDate.of(2026, 8, 13)).differenceDays)
    }

    @Test
    fun `comparison keeps saved estimate before reality exists`() {
        val comparison = DayOverview.compare(estimate.periodStart, CycleBackup(), mapOf(month to estimate))

        assertEquals(EstimateAccuracy.NO_REALITY, comparison?.accuracy)
        assertNull(comparison?.actualStart)
    }

    @Test
    fun `comparison is absent on an unrelated day`() {
        assertNull(DayOverview.compare(month.atDay(20), CycleBackup(), mapOf(month to estimate)))
    }

    @Test
    fun `comparison ignores a period carried over from the previous month`() {
        val september = YearMonth.of(2026, 9)
        val septemberEstimate = estimate.copy(
            month = september,
            periodStart = september.atDay(25),
            earliestStart = september.atDay(23),
            latestStart = september.atDay(27),
        )
        val logs = listOf(30, 31).map { DayLog(month.atDay(it), bleeding = true, flow = Flow.UNKNOWN) } +
            DayLog(september.atDay(1), bleeding = true, flow = Flow.UNKNOWN)

        assertNull(DayOverview.compare(september.atDay(1), CycleBackup(logs = logs), mapOf(september to septemberEstimate)))
    }

    @Test
    fun `comparison is absent without a saved monthly estimate`() {
        assertNull(DayOverview.compare(month.atDay(20), CycleBackup(), emptyMap()))
    }

    @Test
    fun `comparison matches reality across a month boundary`() {
        val snapshot = estimate.copy(
            periodStart = month.atEndOfMonth(),
            earliestStart = month.atEndOfMonth().minusDays(2),
            latestStart = month.atEndOfMonth().plusDays(2),
        )
        val actual = month.plusMonths(1).atDay(1)
        val logs = (0L..2L).map { DayLog(actual.plusDays(it), bleeding = true, flow = Flow.UNKNOWN) }

        val comparison = requireNotNull(DayOverview.compare(actual, CycleBackup(logs = logs), mapOf(month to snapshot)))

        assertEquals(EstimateAccuracy.LATE, comparison.accuracy)
        assertEquals(1, comparison.differenceDays)
        assertEquals(actual, comparison.actualStart)
    }

    private fun compare(actualStart: LocalDate): DayEstimateComparison {
        val logs = (0L..2L).map { offset ->
            DayLog(actualStart.plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
        }
        return requireNotNull(DayOverview.compare(actualStart, CycleBackup(logs = logs), mapOf(month to estimate)))
    }
}
