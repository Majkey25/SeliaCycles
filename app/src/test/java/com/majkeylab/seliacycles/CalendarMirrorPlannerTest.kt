package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarMirrorPlannerTest {
    @Test
    fun groupsRecordedBleedingDaysWithoutExposingPrivateDetails() {
        val start = LocalDate.of(2026, 7, 2)
        val backup = CycleBackup(
            logs = listOf(
                DayLog(start, bleeding = true, flow = Flow.MEDIUM, note = "private note"),
                DayLog(start.plusDays(1), bleeding = true, flow = Flow.LIGHT, symptoms = setOf(Symptom.CRAMPS)),
            ),
            settings = AppSettings(predictionsEnabled = false),
        )

        assertEquals(
            listOf(MirrorEvent(MirrorEventKind.RECORDED, start, start.plusDays(2))),
            CalendarMirrorPlanner.plan(backup, emptyMap(), LocalDate.of(2026, 8, 1)),
        )
    }

    @Test
    fun addsPredictedPeriodSpansWhenPredictionsAreEnabled() {
        val first = LocalDate.of(2026, 6, 1)
        val second = LocalDate.of(2026, 7, 1)
        val bleeding = (0L..2L).flatMap { offset ->
            listOf(first.plusDays(offset), second.plusDays(offset))
        }.map { DayLog(it, bleeding = true, flow = Flow.UNKNOWN) }

        val estimated = CalendarMirrorPlanner.plan(
            CycleBackup(logs = bleeding),
            emptyMap(),
            LocalDate.of(2026, 7, 15),
        ).first { it.kind == MirrorEventKind.ESTIMATED }

        assertEquals(LocalDate.of(2026, 7, 31), estimated.start)
        assertEquals(LocalDate.of(2026, 8, 3), estimated.endExclusive)

        val events = CalendarMirrorPlanner.plan(CycleBackup(logs = bleeding), emptyMap(), LocalDate.of(2026, 7, 15))
        assertEquals(
            MirrorEvent(MirrorEventKind.OVULATION, LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 18)),
            events.first { it.kind == MirrorEventKind.OVULATION },
        )
        assertEquals(
            MirrorEvent(MirrorEventKind.FERTILE, LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 19)),
            events.first { it.kind == MirrorEventKind.FERTILE },
        )
    }

    @Test
    fun limitsRecordedHistoryToTheRollingTwelveMonthWindow() {
        val old = LocalDate.of(2025, 7, 1)
        val recent = LocalDate.of(2025, 8, 1)
        val events = CalendarMirrorPlanner.plan(
            CycleBackup(
                logs = listOf(old, recent).map { DayLog(it, bleeding = true, flow = Flow.UNKNOWN) },
                settings = AppSettings(predictionsEnabled = false),
            ),
            emptyMap(),
            LocalDate.of(2026, 8, 20),
        )

        assertTrue(events.none { it.start == old })
        assertTrue(events.any { it.start == recent })
    }

    @Test
    fun keepsSavedEstimateBesideDifferentRecordedReality() {
        val month = java.time.YearMonth.of(2026, 8)
        val estimated = LocalDate.of(2026, 8, 5)
        val recorded = LocalDate.of(2026, 8, 10)
        val snapshot = ForecastSnapshot(month, estimated, estimated.minusDays(2), estimated.plusDays(2), 3, false)
        val events = CalendarMirrorPlanner.plan(
            backup = CycleBackup(
                logs = listOf(DayLog(recorded, bleeding = true, flow = Flow.UNKNOWN)),
                settings = AppSettings(predictionsEnabled = false),
            ),
            snapshots = mapOf(month to snapshot),
            referenceDate = LocalDate.of(2026, 8, 20),
        )

        assertTrue(events.any { it.kind == MirrorEventKind.ESTIMATED && it.start == estimated })
        assertTrue(events.any { it.kind == MirrorEventKind.RECORDED && it.start == recorded })
    }

    @Test
    fun updatesMatchingEventsAndDeletesOnlyDuplicatesOrStaleRows() {
        val event = MirrorEvent(
            MirrorEventKind.RECORDED,
            LocalDate.of(2026, 8, 28),
            LocalDate.of(2026, 8, 30),
        )

        assertEquals(
            listOf(
                MirrorMutation.Update(10, event),
                MirrorMutation.Delete(11),
                MirrorMutation.Delete(12),
            ),
            CalendarMirrorDiff.plan(
                desired = listOf(event),
                existing = listOf(
                    StoredMirrorEvent(10, event.key),
                    StoredMirrorEvent(11, event.key),
                    StoredMirrorEvent(12, "estimated/2026-07-01"),
                ),
            ),
        )
    }
}
