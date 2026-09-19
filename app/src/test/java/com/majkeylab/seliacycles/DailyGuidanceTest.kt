package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyGuidanceTest {
    private val start = LocalDate.of(2026, 9, 1)
    @Test fun `day context changes during the same bleeding episode`() {
        val backup = CycleBackup((0L..5L).map { DayLog(start.plusDays(it), bleeding = true, flow = Flow.UNKNOWN) })
        listOf(1 to MenstrualStage.EARLY, 3 to MenstrualStage.MIDDLE, 6 to MenstrualStage.LATER).forEach { (day, stage) ->
            val insight = CycleInsights.forDate(backup, emptyMap(), start.plusDays(day - 1L))
            assertEquals(day, insight.menstrualDay)
            assertEquals(stage, insight.menstrualStage)
        }
        assertNull(CycleInsights.forDate(backup, emptyMap(), start.plusDays(10)).menstrualStage)
    }

    @Test fun `shortened confirmed period uses its actual ending rather than average duration`() {
        val logs = (0L..2L).map { DayLog(start.plusDays(it), bleeding = true, flow = Flow.UNKNOWN) }
        val backup = CycleBackup(logs, AppSettings(periodLengthOverride = 7))
        assertEquals(MenstrualStage.LATER, CycleInsights.forDate(backup, emptyMap(), start.plusDays(2)).menstrualStage)
        assertEquals(CyclePhase.FOLLICULAR, CycleInsights.forDate(backup, emptyMap(), start.plusDays(3)).phase)
    }

    @Test fun `personal mood trends compare nearby menstrual days not the whole phase`() {
        val starts = listOf(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1), start)
        val backup = CycleBackup(starts.flatMap { date -> (0L..4L).map { offset ->
            DayLog(date.plusDays(offset), bleeding = true, flow = Flow.UNKNOWN,
                mood = if (date == start) null else when(offset) { 0L -> Mood.LOW; 4L -> Mood.GREAT; else -> null })
        } })
        assertEquals(Mood.LOW, CycleInsights.forDate(backup, emptyMap(), start).moodTrend?.mood)
        assertEquals(Mood.GREAT, CycleInsights.forDate(backup, emptyMap(), start.plusDays(4)).moodTrend?.mood)
    }

    @Test fun `pregnancy does not receive ordinary menstrual stage guidance`() {
        val backup = CycleBackup(listOf(DayLog(start, bleeding = true, flow = Flow.UNKNOWN)),
            AppSettings(profile = UserProfile(lifeSituation = LifeSituation.PREGNANT)))
        assertNull(CycleInsights.forDate(backup, emptyMap(), start).menstrualStage)
    }

    @Test fun `recorded late bleeding mood is not discarded by a shorter duration setting`() {
        val starts = (0L..3L).map { start.minusMonths(it) }
        val backup = CycleBackup(starts.flatMap { date -> (0L..6L).map { offset ->
            DayLog(date.plusDays(offset), bleeding = true, flow = Flow.UNKNOWN,
                mood = Mood.LOW.takeIf { date != start && offset == 6L })
        } }, AppSettings(periodLengthOverride = 3))
        assertEquals(Mood.LOW, CycleInsights.forDate(backup, emptyMap(), start.plusDays(6)).moodTrend?.mood)
    }
}
