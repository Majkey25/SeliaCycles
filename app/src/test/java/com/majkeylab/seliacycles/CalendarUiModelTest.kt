package com.majkeylab.seliacycles

import java.time.DayOfWeek
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.LocalDate

class CalendarUiModelTest {
    @Test
    fun `month grid fills adjacent dates around a Saturday start`() {
        val days = CalendarPaging.gridDays(YearMonth.of(2026, 8), DayOfWeek.MONDAY)

        assertEquals(42, days.size)
        assertEquals(LocalDate.of(2026, 7, 27), days.first())
        assertEquals(LocalDate.of(2026, 8, 1), days[5])
        assertEquals(LocalDate.of(2026, 9, 6), days.last())
    }

    @Test
    fun `period editor follows the configured first weekday`() {
        val base = LocalDate.of(2026, 8, 15)

        val monday = CalendarPaging.periodEditorDays(base, DayOfWeek.MONDAY)
        val sunday = CalendarPaging.periodEditorDays(base, DayOfWeek.SUNDAY)

        assertEquals(LocalDate.of(2026, 8, 3), monday.first())
        assertEquals(LocalDate.of(2026, 8, 2), sunday.first())
        assertEquals(DayOfWeek.MONDAY, monday.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, sunday.first().dayOfWeek)
    }

    @Test
    fun `pager maps the supported month range without drift`() {
        val current = YearMonth.of(2026, 8)

        assertEquals(current, CalendarPaging.monthFor(CalendarPaging.pageFor(current)))
        assertEquals(YearMonth.from(DayLog.MIN_DATE), CalendarPaging.monthFor(0))
        assertEquals(YearMonth.from(DayLog.MAX_DATE), CalendarPaging.monthFor(CalendarPaging.pageCount - 1))
    }

    @Test
    fun `period never erases overlapping fertility and ovulation`() {
        val day = LocalDate.of(2026, 9, 12)

        val tracks = calendarDayTracks(
            day = day,
            recorded = setOf(day),
            predicted = setOf(day),
            fertile = setOf(day),
            ovulation = setOf(day),
        )

        assertEquals(CalendarPeriodLayer.RECORDED, tracks.period)
        assertTrue(tracks.fertile)
        assertTrue(tracks.ovulation)
        assertTrue(tracks.predictedOverlap)
    }

    @Test
    fun `calendar marker shows optional user details but not bleeding alone`() {
        val day = LocalDate.of(2026, 8, 30)

        assertFalse(DayLog(day, bleeding = true, flow = Flow.UNKNOWN).hasCalendarMarker)
        assertTrue(DayLog(day, note = "Back pain").hasCalendarMarker)
        assertTrue(DayLog(day, intimacy = Intimacy.SEX).hasCalendarMarker)
        assertTrue(DayLog(day, painLevel = 8).hasCalendarMarker)
    }

    @Test
    fun `Today dashboard targets exact prediction dates`() {
        val period = LocalDate.of(2026, 9, 11)
        val fertility = CycleInsights.fertilityForPeriod(period)
        val targets = TodayDashboard.targets(DailyCycleInsight(
            nextPeriodStart = period,
            phase = CyclePhase.FOLLICULAR,
            fertility = fertility,
            fertilityStatus = FertilityStatus.OUTSIDE,
            moodTrend = null,
        ))

        assertEquals(period, targets.period)
        assertEquals(fertility.fertileStart, targets.fertile)
        assertEquals(fertility.ovulation, targets.ovulation)
    }

    @Test
    fun `Today dashboard distinguishes upcoming expected and late periods`() {
        assertEquals(PeriodTiming.UPCOMING, TodayDashboard.periodTiming(1))
        assertEquals(PeriodTiming.TODAY, TodayDashboard.periodTiming(0))
        assertEquals(PeriodTiming.LATE, TodayDashboard.periodTiming(-1))
    }

    @Test
    fun `App state derives Today values from one reference date`() {
        val reference = LocalDate.of(2026, 7, 29)
        val state = AppState(
            backup = CycleBackup(logs = (0L..2L).map { offset ->
                DayLog(LocalDate.of(2026, 7, 1).plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
            }),
            referenceDate = reference,
        )

        assertEquals(reference, state.referenceDate)
        assertEquals(LocalDate.of(2026, 7, 29), state.prediction.nextPeriodStart)
        assertEquals(CyclePhase.MENSTRUAL, state.todayInsight.phase)
    }
}
