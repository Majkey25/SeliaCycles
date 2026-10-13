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
    fun `status copies reuse cycle calculations but changed inputs recalculate`() {
        val today = LocalDate.of(2026, 9, 1)
        val content = CycleContent(
            backup = CycleBackup(listOf(DayLog(today, bleeding = true, flow = Flow.UNKNOWN))),
            referenceDate = today,
        )
        val state = AppState(content = content)
        kotlin.test.assertSame(content, state.copy(busy = true).content)
        val edited = state.copy(content = content.copy(backup = content.backup.copy(settings = AppSettings(cycleLengthOverride = 35))))
        assertEquals(today.plusDays(35), edited.prediction.nextPeriodStart)
        assertEquals(today.plusDays(28), state.prediction.nextPeriodStart)
    }

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
    fun `period editor keeps weekday alignment at the earliest supported date`() {
        val days = CalendarPaging.periodEditorDays(DayLog.MIN_DATE, DayOfWeek.SUNDAY)

        assertEquals(DayOfWeek.SUNDAY, days.first().dayOfWeek)
        assertTrue(DayLog.MIN_DATE in days)
        assertEquals(28, days.size)
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
    fun `expired cycle hides fertility in day detail and calendar until a real start is recorded`() {
        val today = LocalDate.of(2026, 9, 12)
        val first = LocalDate.of(2026, 8, 1)
        val backup = CycleBackup((0L..4L).map { DayLog(first.plusDays(it), true, flow = Flow.UNKNOWN) })
        val content = CycleContent(backup = backup, referenceDate = today)
        val calendar = CalendarTracks(content, showFertility = true)

        assertEquals(FertilityStatus.UNAVAILABLE, content.todayInsight.fertilityStatus)
        assertEquals(DailyFertilityLevel.UNAVAILABLE,
            DailyFertility.forDate(today, backup, content.prediction, content.periodEstimates, today))
        assertFalse(calendar.forDay(today).fertile)
        assertFalse(calendar.forDay(today).ovulation)
        val futureOvulation = LocalDate.of(2026, 10, 10)
        assertTrue(calendar.forDay(futureOvulation).ovulation)
        assertEquals(DailyFertilityLevel.ESTIMATED_OVULATION,
            DailyFertility.forDate(futureOvulation, backup, content.prediction, content.periodEstimates, today))
        assertEquals(DailyFertilityLevel.POSSIBLE,
            DailyFertility.forDate(LocalDate.of(2026, 10, 1), backup, content.prediction, content.periodEstimates, today))

        val actual = LocalDate.of(2026, 8, 29)
        val restored = content.copy(backup = backup.copy(logs = backup.logs +
            (0L..4L).map { DayLog(actual.plusDays(it), true, flow = Flow.UNKNOWN) }))
        assertEquals(FertilityStatus.OVULATION, restored.todayInsight.fertilityStatus)
        assertEquals(DailyFertilityLevel.ESTIMATED_OVULATION,
            DailyFertility.forDate(today, restored.backup, restored.prediction, restored.periodEstimates, today))
        assertTrue(CalendarTracks(restored, showFertility = true).forDay(today).ovulation)
    }

    @Test
    fun `disabled predictions cannot restore future calendar snapshots but keep history`() {
        val today = LocalDate.of(2026, 9, 5)
        val future = LocalDate.of(2026, 9, 26)
        val historical = LocalDate.of(2026, 8, 29)
        val snapshots = listOf(historical, future).associate { start ->
            YearMonth.from(start) to ForecastSnapshot(YearMonth.from(start), start,
                start.minusDays(2), start.plusDays(2), 5, false)
        }
        val logs = listOf(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1))
            .map { DayLog(it, true, flow = Flow.UNKNOWN) }
        val settings = listOf(AppSettings(predictionsEnabled = false)) +
            listOf(LifeSituation.PREGNANT, LifeSituation.MENOPAUSE).map {
                AppSettings(profile = UserProfile(lifeSituation = it))
            }

        settings.forEach {
            val content = CycleContent(CycleBackup(logs, it), snapshots, today)
            val calendar = CalendarTracks(content, showFertility = true)
            assertEquals(CalendarPeriodLayer.NONE, calendar.forDay(future).period)
            assertEquals(CalendarPeriodLayer.PREDICTED, calendar.forDay(historical).period)
            assertEquals(CalendarPeriodLayer.RECORDED, calendar.forDay(LocalDate.of(2026, 9, 1)).period)
        }
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
            content = CycleContent(backup = CycleBackup(logs = (0L..2L).map { offset ->
                DayLog(LocalDate.of(2026, 7, 1).plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
            }),
            referenceDate = reference),
        )

        assertEquals(reference, state.referenceDate)
        assertEquals(LocalDate.of(2026, 7, 29), state.prediction.nextPeriodStart)
        assertEquals(CyclePhase.MENSTRUAL, state.todayInsight.phase)
    }

    @Test
    fun `paused predictions retain elapsed current month snapshots without a recorded start`() {
        val historical = LocalDate.of(2026, 9, 10)
        val future = LocalDate.of(2026, 10, 8)
        val snapshots = listOf(historical, future).associate { start ->
            YearMonth.from(start) to ForecastSnapshot(YearMonth.from(start), start,
                start.minusDays(2), start.plusDays(2), 5, false)
        }
        val logs = (0L..4L).map { DayLog(LocalDate.of(2026, 8, 13).plusDays(it), true, flow = Flow.UNKNOWN) }
        val settings = listOf(
            AppSettings(predictionsEnabled = false),
            AppSettings(profile = UserProfile(lifeSituation = LifeSituation.PREGNANT)),
        )

        settings.forEach {
            val content = CycleContent(CycleBackup(logs, it), snapshots, LocalDate.of(2026, 9, 19))
            assertEquals(listOf(historical), content.periodEstimates.map(PeriodEstimate::start))
            val calendar = CalendarTracks(content, showFertility = true)
            assertEquals(CalendarPeriodLayer.PREDICTED, calendar.forDay(historical).period)
            assertEquals(CalendarPeriodLayer.NONE, calendar.forDay(future).period)

            val before = content.copy(referenceDate = LocalDate.of(2026, 9, 5))
            assertTrue(before.periodEstimates.isEmpty())
            assertEquals(CalendarPeriodLayer.NONE, CalendarTracks(before, showFertility = true).forDay(historical).period)
            val nextMonth = content.copy(referenceDate = LocalDate.of(2026, 10, 1))
            assertEquals(listOf(historical), nextMonth.periodEstimates.map(PeriodEstimate::start))
        }
    }
}
