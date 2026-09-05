package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyFertilityTest {
    private val periodStart = LocalDate.of(2026, 9, 29)
    private val reference = periodStart.minusDays(25)
    private val backup = CycleBackup(logs = listOf(DayLog(periodStart.minusDays(28), true, flow = Flow.UNKNOWN)))
    private val prediction = CyclePredictor.predict(setOf(periodStart.minusDays(28)), 28, 5, periodStart.minusDays(25))

    @Test
    fun `central window and uncertainty margins have inclusive boundaries`() {
        val estimates = listOf(estimate(periodStart, 2))
        val expected = mapOf(
            7 to DailyFertilityLevel.OUTSIDE_ESTIMATE,
            8 to DailyFertilityLevel.POSSIBLE,
            9 to DailyFertilityLevel.POSSIBLE,
            10 to DailyFertilityLevel.FERTILE_WINDOW,
            14 to DailyFertilityLevel.FERTILE_WINDOW,
            15 to DailyFertilityLevel.ESTIMATED_OVULATION,
            16 to DailyFertilityLevel.FERTILE_WINDOW,
            17 to DailyFertilityLevel.POSSIBLE,
            18 to DailyFertilityLevel.POSSIBLE,
            19 to DailyFertilityLevel.OUTSIDE_ESTIMATE,
        )

        expected.forEach { (day, level) ->
            assertEquals(level, DailyFertility.forDate(periodStart.withDayOfMonth(day), backup, prediction, estimates, reference))
        }
    }

    @Test
    fun `strongest overlapping estimate wins independent of list order`() {
        val first = estimate(periodStart, 3)
        val second = estimate(periodStart.plusDays(2), 2)
        val date = LocalDate.of(2026, 9, 17)

        assertEquals(DailyFertilityLevel.ESTIMATED_OVULATION,
            DailyFertility.forDate(date, backup, prediction, listOf(first, second), reference))
        assertEquals(DailyFertilityLevel.ESTIMATED_OVULATION,
            DailyFertility.forDate(date, backup, prediction, listOf(second, first), reference))
    }

    @Test
    fun `disabled or medically unsuitable profiles cannot produce fertility guidance`() {
        val settings = listOf(AppSettings(predictionsEnabled = false)) +
            LifeSituation.entries.filter { it != LifeSituation.REGULAR_CYCLES }.map {
                AppSettings(profile = UserProfile(lifeSituation = it))
            }
        settings.forEach {
            assertEquals(DailyFertilityLevel.UNAVAILABLE, DailyFertility.forDate(
                periodStart.minusDays(14), backup.copy(settings = it), prediction, listOf(estimate(periodStart)), reference,
            ))
        }
        assertEquals(DailyFertilityLevel.UNAVAILABLE, DailyFertility.forDate(
            periodStart.minusDays(14), backup.copy(settings = AppSettings(lutealPhaseLength = 19)),
            prediction.copy(averageCycleLength = 19), listOf(estimate(periodStart)), reference,
        ))
    }

    @Test
    fun `no history or no valid estimate is unavailable`() {
        val date = periodStart.minusDays(14)
        assertEquals(DailyFertilityLevel.UNAVAILABLE,
            DailyFertility.forDate(date, CycleBackup(), CyclePredictor.predict(emptySet(), 28, 5, reference), emptyList(), reference))
        assertEquals(DailyFertilityLevel.UNAVAILABLE,
            DailyFertility.forDate(date, backup, prediction, emptyList(), reference))
        val invalid = listOf(
            estimate(periodStart).copy(earliestStart = periodStart.plusDays(1)),
            estimate(periodStart).copy(latestStart = periodStart.minusDays(1)),
            estimate(periodStart).copy(endExclusive = periodStart),
            estimate(periodStart).copy(start = LocalDate.MIN),
        )
        assertEquals(DailyFertilityLevel.UNAVAILABLE,
            DailyFertility.forDate(date, backup, prediction, invalid, reference))
    }

    @Test
    fun `luteal preference moves all boundaries together and absent uncertainty adds no margins`() {
        val custom = backup.copy(settings = AppSettings(lutealPhaseLength = 12))
        val estimates = listOf(estimate(periodStart).copy(earliestStart = null, latestStart = null))
        val month = DailyFertility.forMonth(YearMonth.of(2026, 9), custom, prediction, estimates, reference)

        assertEquals(30, month.size)
        assertEquals(DailyFertilityLevel.OUTSIDE_ESTIMATE, month[LocalDate.of(2026, 9, 11)])
        assertEquals(DailyFertilityLevel.FERTILE_WINDOW, month[LocalDate.of(2026, 9, 12)])
        assertEquals(DailyFertilityLevel.ESTIMATED_OVULATION, month[LocalDate.of(2026, 9, 17)])
        assertEquals(DailyFertilityLevel.FERTILE_WINDOW, month[LocalDate.of(2026, 9, 18)])
        assertEquals(DailyFertilityLevel.OUTSIDE_ESTIMATE, month[LocalDate.of(2026, 9, 19)])
        assertTrue(month.values.none { it == DailyFertilityLevel.POSSIBLE })
    }

    @Test
    fun `widening forecast uncertainty expands possible days without changing central window`() {
        val estimates = CycleInsights.calendarPeriodEstimates(backup, emptyMap(), periodStart.minusDays(25))
        val first = estimates.first()
        val distant = estimates.last()
        assertTrue(distant.start.toEpochDay() - requireNotNull(distant.earliestStart).toEpochDay() >
            first.start.toEpochDay() - requireNotNull(first.earliestStart).toEpochDay())
        val edge = requireNotNull(distant.earliestStart).minusDays(19)
        assertEquals(DailyFertilityLevel.POSSIBLE,
            DailyFertility.forDate(edge, backup, prediction, listOf(distant), reference))
        assertEquals(DailyFertilityLevel.OUTSIDE_ESTIMATE,
            DailyFertility.forDate(edge.minusDays(1), backup, prediction, listOf(distant), reference))
    }

    private fun estimate(start: LocalDate, uncertainty: Long = 0): PeriodEstimate = PeriodEstimate(
        start, start.plusDays(5), start.minusDays(uncertainty), start.plusDays(uncertainty), EstimateOrigin.CURRENT,
    )

    @Test
    fun `saved history future records active periods and late forecasts match calendar fertility`() {
        val starts = listOf(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 29), LocalDate.of(2026, 9, 26))
        val history = CycleBackup(logs = starts.map { DayLog(it, true, flow = Flow.UNKNOWN) })
        val frozenStart = LocalDate.of(2026, 9, 25)
        val frozen = ForecastSnapshot(YearMonth.from(frozenStart), frozenStart,
            frozenStart.minusDays(2), frozenStart.plusDays(2), 5, false)
        val snapshots = mapOf(frozen.month to frozen)
        assertCalendarAgreement(history, snapshots, LocalDate.of(2026, 10, 1))
        assertCalendarAgreement(CycleBackup(), snapshots, LocalDate.of(2026, 10, 1))
        assertCalendarAgreement(history, snapshots, LocalDate.of(2026, 9, 1))
        assertCalendarAgreement(backup, emptyMap(), reference)
        assertCalendarAgreement(backup.copy(settings = AppSettings(activePeriodStart = backup.logs.single().day)),
            emptyMap(), reference)
        assertCalendarAgreement(backup, emptyMap(), LocalDate.of(2026, 10, 3))
        val short = history.copy(
            logs = history.logs.dropLast(1) + DayLog(LocalDate.of(2026, 9, 13), true, flow = Flow.UNKNOWN),
            settings = AppSettings(lutealPhaseLength = 19),
        )
        assertCalendarAgreement(short, snapshots, LocalDate.of(2026, 9, 1))
    }

    @Test
    fun `dates without temporal coverage or outside supported dates remain unavailable`() {
        val estimates = listOf(estimate(periodStart))
        listOf(LocalDate.MIN, DayLog.MIN_DATE.minusDays(1), reference.minusYears(1),
            reference.plusYears(2), DayLog.MAX_DATE.plusDays(1), LocalDate.MAX).forEach { date ->
            assertEquals(DailyFertilityLevel.UNAVAILABLE,
                DailyFertility.forDate(date, backup, prediction, estimates, reference))
        }
        assertEquals(DailyFertilityLevel.UNAVAILABLE,
            DailyFertility.forDate(reference, backup, prediction, estimates, LocalDate.MAX))
    }

    @Test
    fun `generated next year forecast still covers fertile dates at the supported upper boundary`() {
        val history = CycleBackup(logs = listOf(DayLog(DayLog.MAX_DATE.minusDays(14), true, flow = Flow.UNKNOWN)))
        assertCalendarAgreement(history, emptyMap(), DayLog.MAX_DATE.minusDays(10))
    }

    private fun assertCalendarAgreement(
        history: CycleBackup,
        snapshots: Map<YearMonth, ForecastSnapshot>,
        referenceDate: LocalDate,
    ) {
        val prediction = CyclePredictor.predict(
            history.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day),
            history.settings.cycleLength, history.settings.periodLength, referenceDate,
            history.settings.cycleLengthOverride, history.settings.periodLengthOverride, history.settings.activePeriodStart,
        )
        val estimates = CycleInsights.calendarPeriodEstimates(history, snapshots, referenceDate)
        val central = CycleInsights.fertilityEstimates(history, snapshots, referenceDate)
        val start = maxOf(DayLog.MIN_DATE, referenceDate.minusDays(35))
        val end = minOf(DayLog.MAX_DATE, referenceDate.plusDays(65))
        generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.forEach { date ->
            val level = DailyFertility.forDate(date, history, prediction, estimates, referenceDate)
            val expected = when {
                central.any { date == it.ovulation } -> DailyFertilityLevel.ESTIMATED_OVULATION
                central.any { date in it.fertileStart..it.fertileEnd } -> DailyFertilityLevel.FERTILE_WINDOW
                else -> null
            }
            if (expected != null) assertEquals(expected, level, "Daily/calendar mismatch on $date at $referenceDate")
            else assertTrue(level !in listOf(DailyFertilityLevel.ESTIMATED_OVULATION, DailyFertilityLevel.FERTILE_WINDOW),
                "Daily-only central window on $date at $referenceDate")
        }
    }
}
