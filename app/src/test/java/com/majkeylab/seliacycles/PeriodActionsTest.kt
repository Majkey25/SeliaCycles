package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `replace changes exact period days and preserves optional details and other periods`() {
        val firstPeriod = PeriodActions.end(start.plusDays(4), PeriodActions.start(start, emptyList()), start).map { log ->
            if (log.day == start.plusDays(1)) log.copy(note = "keep", mood = Mood.GOOD) else log
        }
        val otherStart = start.plusDays(30)
        val logs = PeriodActions.end(otherStart.plusDays(2), PeriodActions.start(otherStart, firstPeriod), otherStart)
        val selected = setOf(start.minusDays(1), start, start.plusDays(1), start.plusDays(2))

        val result = PeriodActions.replace(start, selected, logs, today = otherStart.plusDays(10))

        assertEquals(
            selected + (0L..2L).map(otherStart::plusDays),
            result.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day),
        )
        assertEquals("keep", result.single { it.day == start.plusDays(1) }.note)
        assertEquals(Mood.GOOD, result.single { it.day == start.plusDays(1) }.mood)
    }

    @Test
    fun `replace with no selected days removes only bleeding and keeps the day information`() {
        val logs = PeriodActions.end(start.plusDays(2), PeriodActions.start(start, emptyList()), start).map { log ->
            if (log.day == start.plusDays(1)) log.copy(symptoms = setOf(Symptom.CRAMPS)) else log
        }

        val result = PeriodActions.replace(start, emptySet(), logs, today = start.plusDays(10))

        assertTrue(result.none(DayLog::bleeding))
        assertEquals(setOf(Symptom.CRAMPS), result.single().symptoms)
    }

    @Test
    fun `replace rejects future or longer than fourteen day selections`() {
        val today = start.plusDays(5)

        assertFailsWith<IllegalArgumentException> {
            PeriodActions.replace(start, setOf(today.plusDays(1)), emptyList(), today)
        }
        assertFailsWith<IllegalArgumentException> {
            PeriodActions.replace(start, setOf(start, start.plusDays(14)), emptyList(), today.plusDays(20))
        }
    }

    @Test
    fun `future bleeding is removed while other future details stay`() {
        val today = start
        val future = start.plusDays(1)

        val result = PeriodActions.removeFutureBleeding(
            listOf(DayLog(today, bleeding = true, flow = Flow.UNKNOWN), DayLog(future, bleeding = true, flow = Flow.HEAVY, note = "keep")),
            today,
        )

        assertTrue(result.single { it.day == today }.bleeding)
        assertEquals(DayLog(future, note = "keep"), result.single { it.day == future })
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
