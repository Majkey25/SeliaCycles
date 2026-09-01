package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PredictionUpdateAcceptanceTest {
    private val reference = LocalDate.of(2026, 9, 1)
    private val staleStart = LocalDate.of(2026, 9, 26)
    private val snapshot = ForecastSnapshot(
        month = YearMonth.of(2026, 9),
        periodStart = staleStart,
        earliestStart = staleStart.minusDays(1),
        latestStart = staleStart.plusDays(1),
        periodLength = 5,
        reconstructed = false,
    )
    private val settings = AppSettings(
        cycleLength = 30,
        periodLength = 5,
        cycleLengthOverride = 30,
        periodLengthOverride = 5,
    )

    @Test
    fun `new real start reanchors every future consumer without reviving the stale baseline`() {
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 7, 14)) +
                period(LocalDate.of(2026, 8, 13)) +
                period(reference),
            settings = settings,
        )
        val snapshots = mapOf(snapshot.month to snapshot)

        val estimates = CycleInsights.calendarPeriodEstimates(backup, snapshots, reference)
        val fertility = CycleInsights.fertilityEstimates(backup, snapshots, reference)
        val mirrored = CalendarMirrorPlanner.plan(backup, snapshots, reference)

        assertFalse(estimates.any { it.start == staleStart })
        assertEquals(
            listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            estimates.filter { it.start.isAfter(reference) }.take(2).map(PeriodEstimate::start),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            fertility.filter { it.periodStart.isAfter(reference) }.take(2).map(FertilityEstimate::periodStart),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            mirrored.filter { it.kind == MirrorEventKind.ESTIMATED && it.start.isAfter(reference) }
                .take(2).map(MirrorEvent::start),
        )
    }

    @Test
    fun `editing or removing the new start moves the following month`() {
        val original = period(LocalDate.of(2026, 7, 14)) + period(LocalDate.of(2026, 8, 13))
        val movedStart = LocalDate.of(2026, 9, 3)
        val moved = CycleBackup(logs = original + period(movedStart), settings = settings)
        val removed = CycleBackup(logs = original, settings = settings)

        assertEquals(
            LocalDate.of(2026, 10, 3),
            CycleInsights.forDate(moved, mapOf(snapshot.month to snapshot), movedStart).nextPeriodStart,
        )
        assertEquals(
            LocalDate.of(2026, 9, 12),
            CycleInsights.forDate(removed, mapOf(snapshot.month to snapshot), reference).nextPeriodStart,
        )
    }

    private fun period(start: LocalDate): List<DayLog> = (0L..4L).map { offset ->
        DayLog(start.plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
    }
}
