package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForecastSnapshotPlannerTest {
    @Test
    fun reconstructsPastEstimateWithoutUsingThatMonthsRealPeriod() {
        val backup = CycleBackup(logs = period(LocalDate.of(2026, 6, 1)) +
            period(LocalDate.of(2026, 7, 2)) + period(LocalDate.of(2026, 8, 7)))

        val august = ForecastSnapshotPlanner.missingSnapshots(
            backup = backup,
            existing = emptyMap(),
            referenceDate = LocalDate.of(2026, 8, 20),
        ).first { it.month == YearMonth.of(2026, 8) }

        assertEquals(LocalDate.of(2026, 8, 2), august.periodStart)
        assertTrue(august.reconstructed)
    }

    @Test
    fun neverReplacesAnExistingMonthlyBaseline() {
        val month = YearMonth.of(2026, 8)
        val existing = ForecastSnapshot(
            month = month,
            periodStart = LocalDate.of(2026, 8, 4),
            earliestStart = LocalDate.of(2026, 8, 2),
            latestStart = LocalDate.of(2026, 8, 6),
            periodLength = 5,
            reconstructed = false,
        )

        val missing = ForecastSnapshotPlanner.missingSnapshots(
            backup = CycleBackup(logs = period(LocalDate.of(2026, 6, 1)) + period(LocalDate.of(2026, 7, 1))),
            existing = mapOf(month to existing),
            referenceDate = LocalDate.of(2026, 8, 1),
        )

        assertFalse(missing.any { it.month == month })
    }

    @Test
    fun storesNoFutureMonthsAndNoEstimateWithoutPriorData() {
        val reference = LocalDate.of(2026, 8, 20)
        val snapshots = ForecastSnapshotPlanner.missingSnapshots(
            backup = CycleBackup(),
            existing = emptyMap(),
            referenceDate = reference,
        )

        assertTrue(snapshots.isEmpty())
        assertTrue(snapshots.none { it.month > YearMonth.from(reference) })
    }

    private fun period(start: LocalDate): List<DayLog> = (0L..2L).map { offset ->
        DayLog(start.plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
    }
}
