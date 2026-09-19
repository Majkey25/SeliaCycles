package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FertilityRecalculationTest {
    private val reference = LocalDate.of(2026, 9, 19)
    private val first = LocalDate.of(2026, 8, 1)
    private val oldStart = LocalDate.of(2026, 8, 29)
    private val snapshot = ForecastSnapshot(YearMonth.from(oldStart), oldStart, oldStart.minusDays(2), oldStart.plusDays(2), 5, false)
    private val snapshots = mapOf(snapshot.month to snapshot)

    @Test fun `recorded period corrects historical fertility without rewriting original forecast`() {
        val actual = LocalDate.of(2026, 8, 31)
        val backup = CycleBackup(period(first) + period(actual))
        val windows = CycleInsights.fertilityEstimates(backup, snapshots, reference)
        assertEquals(LocalDate.of(2026, 8, 17), windows.single { it.periodStart == actual }.ovulation)
        assertFalse(windows.any { it.periodStart == oldStart })
        val insight = CycleInsights.forDate(backup, snapshots, LocalDate.of(2026, 8, 17), reference)
        assertEquals(FertilityStatus.OVULATION, insight.fertilityStatus)
        assertEquals(actual, insight.fertility?.periodStart)
        val prediction = CycleInsights.prediction(backup, reference)
        val estimates = CycleInsights.calendarPeriodEstimates(backup, snapshots, reference)
        assertEquals(DailyFertilityLevel.ESTIMATED_OVULATION,
            DailyFertility.forDate(LocalDate.of(2026, 8, 17), backup, prediction, estimates, reference))
        assertEquals(DailyFertilityLevel.FERTILE_WINDOW,
            DailyFertility.forDate(LocalDate.of(2026, 8, 15), backup, prediction, estimates, reference))
        assertEquals(oldStart, CycleInsights.periodEstimates(backup, snapshots, reference)
            .single { it.origin == EstimateOrigin.SAVED }.start)
    }

    @Test fun `moving the current period moves current cycle ovulation and next period together`() {
        val original = CycleBackup(period(first) + period(oldStart))
        val moved = CycleBackup(period(first) + period(LocalDate.of(2026, 9, 2)))
        assertEquals(LocalDate.of(2026, 9, 12), CycleInsights.forDate(original, snapshots, LocalDate.of(2026, 9, 5), reference).fertility?.ovulation)
        assertEquals(LocalDate.of(2026, 9, 20), CycleInsights.forDate(moved, snapshots, LocalDate.of(2026, 9, 5), reference).fertility?.ovulation)
        assertEquals(LocalDate.of(2026, 10, 4), CycleInsights.prediction(moved, reference).nextPeriodStart)
    }

    private fun period(start: LocalDate) = (0L..4L).map { DayLog(start.plusDays(it), bleeding = true, flow = Flow.UNKNOWN) }

    @Test fun `cycle length counts elapsed days across leap and year boundaries`() {
        listOf(
            LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29),
            LocalDate.of(2026, 2, 1) to LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 12, 20) to LocalDate.of(2027, 1, 17),
        ).forEach { (start, expected) ->
            val backup = CycleBackup(period(start), AppSettings(cycleLengthOverride = 28))
            assertEquals(expected, CycleInsights.prediction(backup, start).nextPeriodStart)
        }
    }

    @Test fun `changing bleeding duration alone does not move the ovulation estimate`() {
        val backup = CycleBackup(period(first) + period(oldStart))
        val shorter = backup.copy(settings = backup.settings.copy(periodLengthOverride = 3))
        assertEquals(CycleInsights.forDate(backup, snapshots, reference).fertility,
            CycleInsights.forDate(shorter, snapshots, reference).fertility)
    }
}
