package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivePeriodEstimateTest {
    private val start = LocalDate.of(2026, 8, 30)
    private val backup = CycleBackup(
        logs = listOf(DayLog(start, bleeding = true, flow = Flow.UNKNOWN)),
        settings = AppSettings(activePeriodStart = start, periodLength = 5),
    )

    @Test fun `started period covers five days across month boundary without inventing records`() {
        val estimates = CycleInsights.calendarPeriodEstimates(backup, emptyMap(), start.plusDays(2))
        val ongoing = estimates.single { it.start == start }
        assertEquals(LocalDate.of(2026, 9, 4), ongoing.endExclusive)
        assertEquals(1, backup.logs.count(DayLog::bleeding))
        val insight = CycleInsights.forDate(backup, emptyMap(), start.plusDays(2))
        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
        assertEquals(LocalDate.of(2026, 9, 27), insight.nextPeriodStart)
        assertEquals(LocalDate.of(2026, 9, 13), insight.fertility?.ovulation)
    }

    @Test fun `history supplies duration and manual setting takes precedence`() {
        val history = (0L..5L).map { DayLog(start.minusDays(28).plusDays(it), bleeding = true, flow = Flow.UNKNOWN) }
        val learned = backup.copy(logs = history + backup.logs)
        assertEquals(start.plusDays(6), CycleInsights.calendarPeriodEstimates(learned, emptyMap(), start)
            .single { it.start == start }.endExclusive)
        val manual = learned.copy(settings = learned.settings.copy(periodLengthOverride = 3))
        assertEquals(start.plusDays(3), CycleInsights.calendarPeriodEstimates(manual, emptyMap(), start)
            .single { it.start == start }.endExclusive)
    }

    @Test fun `ending or removing the period clears its continuation without changing saved forecasts`() {
        val snapshot = ForecastSnapshot(YearMonth.from(start), start.minusDays(1), start.minusDays(2), start, 4, false)
        val snapshots = mapOf(snapshot.month to snapshot)
        val ended = backup.copy(settings = backup.settings.copy(activePeriodStart = null))
        assertTrue(CycleInsights.calendarPeriodEstimates(ended, emptyMap(), start).none { it.start == start })
        assertTrue(CycleInsights.calendarPeriodEstimates(backup.copy(logs = emptyList()), emptyMap(), start).none { it.start == start })
        assertTrue(CycleInsights.calendarPeriodEstimates(backup, snapshots, start).any { it.start == start })
        assertEquals(start.minusDays(1), CycleInsights.periodEstimates(backup, snapshots, start)
            .single { it.origin == EstimateOrigin.SAVED }.start)
    }

    @Test fun `expired active start does not create a perpetual continuation`() {
        assertTrue(CycleInsights.calendarPeriodEstimates(backup, emptyMap(), start.plusDays(5)).none { it.start == start })
        val paused = backup.copy(settings = backup.settings.copy(profile = UserProfile(lifeSituation = LifeSituation.PREGNANT)))
        assertTrue(CycleInsights.calendarPeriodEstimates(paused, emptyMap(), start).none { it.start == start })
    }
}
