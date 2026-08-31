package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PeriodActionsTest {
    private val start = LocalDate.of(2026, 8, 10)

    @Test
    fun `start records only the real first day and preserves existing details`() {
        val moodDay = DayLog(start.plusDays(1), mood = Mood.GOOD)

        val result = PeriodActions.start(start, listOf(moodDay))

        assertEquals(listOf(start), result.filter(DayLog::bleeding).map(DayLog::day))
        assertEquals(Mood.GOOD, result.first { it.day == moodDay.day }.mood)
        assertTrue(result.filter(DayLog::bleeding).all { it.flow == Flow.UNKNOWN })
    }

    @Test
    fun `end fills only the completed range without deleting bonus data`() {
        val logs = PeriodActions.start(start, listOf(DayLog(start.plusDays(1), note = "keep")))

        val result = PeriodActions.end(start.plusDays(2), logs, suggestedStart = start)

        assertEquals((0L..2L).map(start::plusDays), result.filter(DayLog::bleeding).map(DayLog::day))
        assertEquals("keep", result.first { it.day == start.plusDays(1) }.note)
    }

    @Test
    fun `end confirms predicted start when no real start was logged`() {
        val end = start.plusDays(3)

        val result = PeriodActions.end(end, emptyList(), suggestedStart = start)

        assertEquals((0L..3L).map(start::plusDays), result.filter(DayLog::bleeding).map(DayLog::day))
    }

    @Test
    fun `end without recorded or predicted start changes nothing`() {
        val existing = listOf(DayLog(start, mood = Mood.OKAY))

        assertEquals(existing, PeriodActions.end(start, existing, suggestedStart = null))
    }

    @Test
    fun `remove clears one connected period and preserves other details`() {
        val firstPeriod = PeriodActions.end(start.plusDays(4), PeriodActions.start(start, emptyList()), start).map { log ->
            if (log.day == start.plusDays(1)) log.copy(note = "keep") else log
        }
        val otherStart = start.plusDays(30)
        val logs = PeriodActions.end(otherStart.plusDays(3), PeriodActions.start(otherStart, firstPeriod), otherStart)

        val result = PeriodActions.remove(start, logs)

        assertTrue(result.none { it.day in start..start.plusDays(4) && it.bleeding })
        assertEquals("keep", result.single { it.day == start.plusDays(1) }.note)
        assertEquals((0L..3L).map(otherStart::plusDays), result.filter(DayLog::bleeding).map(DayLog::day))
    }

    @Test
    fun `today action respects paused reproductive states`() {
        assertEquals(TodayPrimaryAction.START_PERIOD, PeriodActions.todayAction(AppSettings(), start))
        assertEquals(
            TodayPrimaryAction.END_PERIOD,
            PeriodActions.todayAction(AppSettings(activePeriodStart = start), start.plusDays(2)),
        )
        assertEquals(
            TodayPrimaryAction.START_PERIOD,
            PeriodActions.todayAction(AppSettings(activePeriodStart = start), start.plusDays(14)),
        )
        assertEquals(
            TodayPrimaryAction.OPEN_LOG,
            PeriodActions.todayAction(
                AppSettings(profile = UserProfile(lifeSituation = LifeSituation.PREGNANT)),
                day = start,
            ),
        )
        assertEquals(
            TodayPrimaryAction.OPEN_LOG,
            PeriodActions.todayAction(
                AppSettings(profile = UserProfile(lifeSituation = LifeSituation.MENOPAUSE)),
                day = start,
            ),
        )
    }
}
