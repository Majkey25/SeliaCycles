package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    private fun periodDays(vararg starts: LocalDate): Set<LocalDate> =
        starts.flatMap { start -> (0L..4L).map(start::plusDays) }.toSet()
}
