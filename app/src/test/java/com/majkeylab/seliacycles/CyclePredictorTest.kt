package com.majkeylab.seliacycles

import java.time.LocalDate
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

        val result = CyclePredictor.predict(days, defaultCycleLength = 30, defaultPeriodLength = 6)

        assertEquals(28, result.averageCycleLength)
        assertEquals(5, result.averagePeriodLength)
        assertEquals(LocalDate.of(2026, 7, 24), result.nextPeriodStart)
    }

    @Test
    fun ignoresSingleDayGapInsideOnePeriod() {
        val days = periodDays(LocalDate.of(2026, 7, 1)).toMutableSet()
        days.remove(LocalDate.of(2026, 7, 3))
        days += LocalDate.of(2026, 7, 6)

        val result = CyclePredictor.predict(days, defaultCycleLength = 28, defaultPeriodLength = 5)

        assertEquals(6, result.averagePeriodLength)
        assertEquals(LocalDate.of(2026, 7, 29), result.nextPeriodStart)
    }

    @Test
    fun returnsNoDateWithoutRecordedPeriod() {
        val result = CyclePredictor.predict(emptySet(), defaultCycleLength = 28, defaultPeriodLength = 5)

        assertNull(result.nextPeriodStart)
        assertEquals(28, result.averageCycleLength)
        assertEquals(5, result.averagePeriodLength)
    }

    @Test
    fun weightsRecentCyclesMoreHeavily() {
        val days = periodDays(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 3, 2),
            LocalDate.of(2026, 3, 28),
        )

        val result = CyclePredictor.predict(days, defaultCycleLength = 30, defaultPeriodLength = 5)

        assertEquals(28, result.averageCycleLength)
    }

    @Test
    fun exposesWiderWindowForVariableCycles() {
        val stable = CyclePredictor.predict(periodDays(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 26),
        ), 28, 5)
        val variable = CyclePredictor.predict(periodDays(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 25),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 24),
            LocalDate.of(2026, 4, 27),
        ), 28, 5)

        assertTrue(variable.uncertaintyDays > stable.uncertaintyDays)
        assertEquals(variable.nextPeriodStart?.minusDays(variable.uncertaintyDays.toLong()), variable.earliestPeriodStart)
        assertEquals(variable.nextPeriodStart?.plusDays(variable.uncertaintyDays.toLong()), variable.latestPeriodStart)
    }

    private fun periodDays(vararg starts: LocalDate): Set<LocalDate> =
        starts.flatMap { start -> (0L..4L).map(start::plusDays) }.toSet()
}
