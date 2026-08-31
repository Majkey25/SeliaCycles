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
}
