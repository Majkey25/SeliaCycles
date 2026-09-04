package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CyclePredictorTest {
    @Test
    fun predictsFromRecentCompleteCycles() {
        val days = periodDays(
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 29),
            LocalDate.of(2026, 6, 26),
        )

        val result = CyclePredictor.predict(
            days,
            defaultCycleLength = 30,
            defaultPeriodLength = 6,
            referenceDate = LocalDate.of(2026, 7, 1),
        )

        assertEquals(28, result.averageCycleLength)
        assertEquals(5, result.averagePeriodLength)
        assertEquals(LocalDate.of(2026, 7, 24), result.nextPeriodStart)
    }

    @Test
    fun `keeps expected period current through its estimated span`() {
        val days = periodDays(
            LocalDate.of(2026, 6, 26),
            LocalDate.of(2026, 7, 24),
        )

        val duringSpan = CyclePredictor.predict(days, 28, 5, LocalDate.of(2026, 8, 25))
        val afterSpan = CyclePredictor.predict(days, 28, 5, LocalDate.of(2026, 8, 26))

        assertEquals(LocalDate.of(2026, 8, 21), duringSpan.nextPeriodStart)
        assertEquals(LocalDate.of(2026, 9, 18), afterSpan.nextPeriodStart)
    }

    @Test
    fun `keeps a late expected period across a month boundary`() {
        val days = periodDays(
            LocalDate.of(2026, 7, 5),
            LocalDate.of(2026, 8, 2),
        )

        val result = CyclePredictor.predict(days, 28, 5, LocalDate.of(2026, 9, 1))

        assertEquals(LocalDate.of(2026, 8, 30), result.nextPeriodStart)
    }

    @Test
    fun ignoresSingleDayGapInsideOnePeriod() {
        val days = periodDays(LocalDate.of(2026, 7, 1)).toMutableSet()
        days.remove(LocalDate.of(2026, 7, 3))
        days += LocalDate.of(2026, 7, 6)

        val result = CyclePredictor.predict(
            days,
            defaultCycleLength = 28,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 7, 1),
        )

        assertEquals(6, result.averagePeriodLength)
        assertEquals(LocalDate.of(2026, 7, 29), result.nextPeriodStart)
    }

    @Test
    fun returnsNoDateWithoutRecordedPeriod() {
        val result = CyclePredictor.predict(
            emptySet(),
            defaultCycleLength = 28,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 8, 28),
        )

        assertNull(result.nextPeriodStart)
        assertEquals(28, result.averageCycleLength)
        assertEquals(5, result.averagePeriodLength)
        assertEquals(
            listOf(ForecastStatus.UNAVAILABLE, ForecastStatus.UNAVAILABLE),
            result.monthlyForecasts.map(MonthlyForecast::status),
        )
    }

    @Test
    fun weightsRecentCyclesMoreHeavily() {
        val days = periodDays(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 3, 2),
            LocalDate.of(2026, 3, 28),
        )

        val result = CyclePredictor.predict(
            days,
            defaultCycleLength = 30,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 4, 1),
        )

        assertEquals(28, result.averageCycleLength)
    }

    @Test
    fun exposesWiderWindowForVariableCycles() {
        val stable = CyclePredictor.predict(periodDays(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 26),
        ), 28, 5, LocalDate.of(2026, 4, 1))
        val variable = CyclePredictor.predict(periodDays(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 25),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 24),
            LocalDate.of(2026, 4, 27),
        ), 28, 5, LocalDate.of(2026, 5, 1))

        assertTrue(variable.uncertaintyDays > stable.uncertaintyDays)
        assertEquals(variable.nextPeriodStart?.minusDays(variable.uncertaintyDays.toLong()), variable.earliestPeriodStart)
        assertEquals(variable.nextPeriodStart?.plusDays(variable.uncertaintyDays.toLong()), variable.latestPeriodStart)
    }

    @Test
    fun normalizesSkippedTrackingCycles() {
        val result = CyclePredictor.predict(
            bleedingDays = periodDays(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 29),
                LocalDate.of(2026, 3, 26),
                LocalDate.of(2026, 6, 18),
            ),
            defaultCycleLength = 30,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 6, 20),
        )

        assertEquals(28, result.averageCycleLength)
        assertEquals(LocalDate.of(2026, 7, 16), result.nextPeriodStart)
    }

    @Test
    fun rejectsOneCycleOutlier() {
        val result = CyclePredictor.predict(
            bleedingDays = periodDays(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 29),
                LocalDate.of(2026, 2, 26),
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 5, 8),
            ),
            defaultCycleLength = 30,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 5, 10),
        )

        assertEquals(28, result.averageCycleLength)
    }

    @Test
    fun realPeriodStartReanchorsFuturePrediction() {
        val originalDays = periodDays(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 26),
        )
        val original = CyclePredictor.predict(originalDays, 28, 5, LocalDate.of(2026, 3, 1))
        val updated = CyclePredictor.predict(
            originalDays + periodDays(LocalDate.of(2026, 4, 2)),
            28,
            5,
            LocalDate.of(2026, 4, 2),
        )

        assertEquals(LocalDate.of(2026, 3, 26), original.nextPeriodStart)
        assertEquals(LocalDate.of(2026, 4, 30), updated.nextPeriodStart)
    }

    @Test
    fun exposesForecastForCurrentAndNextMonth() {
        val result = CyclePredictor.predict(
            bleedingDays = periodDays(
                LocalDate.of(2026, 6, 26),
                LocalDate.of(2026, 7, 24),
            ),
            defaultCycleLength = 28,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 8, 1),
        )

        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9)), result.monthlyForecasts.map(MonthlyForecast::month))
        assertEquals(listOf(ForecastStatus.ESTIMATED, ForecastStatus.ESTIMATED), result.monthlyForecasts.map(MonthlyForecast::status))
        assertEquals(listOf(LocalDate.of(2026, 8, 21), LocalDate.of(2026, 9, 18)), result.monthlyForecasts.map(MonthlyForecast::start))
    }

    @Test
    fun currentMonthUsesRecordedPeriodAndNextMonthUsesNewAnchor() {
        val result = CyclePredictor.predict(
            bleedingDays = periodDays(
                LocalDate.of(2026, 6, 26),
                LocalDate.of(2026, 7, 24),
                LocalDate.of(2026, 8, 21),
            ),
            defaultCycleLength = 28,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 8, 21),
        )

        assertEquals(ForecastStatus.RECORDED, result.monthlyForecasts[0].status)
        assertEquals(LocalDate.of(2026, 8, 21), result.monthlyForecasts[0].start)
        assertEquals(ForecastStatus.ESTIMATED, result.monthlyForecasts[1].status)
        assertEquals(LocalDate.of(2026, 9, 18), result.monthlyForecasts[1].start)
    }

    @Test
    fun distinguishesNoExpectedPeriodFromMissingHistory() {
        val result = CyclePredictor.predict(
            bleedingDays = periodDays(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1),
            ),
            defaultCycleLength = 90,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 5, 1),
        )

        assertEquals(ForecastStatus.NOT_EXPECTED, result.monthlyForecasts[0].status)
        assertEquals(ForecastStatus.ESTIMATED, result.monthlyForecasts[1].status)
    }

    @Test
    fun periodCrossingBoundaryIsRecordedInCurrentMonth() {
        val result = CyclePredictor.predict(
            bleedingDays = periodDays(
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 30),
            ),
            defaultCycleLength = 28,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 8, 1),
        )

        assertEquals(ForecastStatus.RECORDED, result.monthlyForecasts[0].status)
        assertEquals(LocalDate.of(2026, 7, 30), result.monthlyForecasts[0].start)
        assertEquals(LocalDate.of(2026, 8, 3), result.monthlyForecasts[0].end)
    }

    @Test
    fun manualCycleLengthOverridesImportedHistoryForFutureMonths() {
        val result = CyclePredictor.predict(
            bleedingDays = periodDays(
                LocalDate.of(2026, 3, 26),
                LocalDate.of(2026, 4, 23),
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 7, 9),
            ),
            defaultCycleLength = 28,
            defaultPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 8, 31),
            cycleLengthOverride = 32,
        )

        assertEquals(32, result.averageCycleLength)
        assertEquals(LocalDate.of(2026, 9, 11), result.nextPeriodStart)
    }

    private fun periodDays(vararg starts: LocalDate): Set<LocalDate> =
        starts.flatMap { start -> (0L..4L).map(start::plusDays) }.toSet()
}
