package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PeriodEndSuggestionTest {
    @Test
    fun `end action is offered only for an ongoing recorded or covering estimated period`() {
        val start = LocalDate.of(2026, 8, 25)
        val logs = (0L..4L).map { DayLog(start.plusDays(it), bleeding = true, flow = Flow.UNKNOWN) }
        val settings = AppSettings()
        assertNull(PeriodActions.suggestedStart(start.plusDays(11), settings, logs, emptyList()))
        assertEquals(start, PeriodActions.suggestedStart(start.plusDays(4), settings, logs, emptyList()))
        assertEquals(start, PeriodActions.suggestedStart(start.plusDays(6), settings.copy(activePeriodStart = start), logs, emptyList()))
        val estimate = PeriodEstimate(start, start.plusDays(5), start, start, EstimateOrigin.CURRENT)
        assertEquals(start, PeriodActions.suggestedStart(start.plusDays(3), settings, emptyList(), listOf(estimate)))
        assertNull(PeriodActions.suggestedStart(start.plusDays(5), settings, emptyList(), listOf(estimate)))
        assertNull(PeriodActions.suggestedStart(start.plusDays(3), settings.copy(predictionsEnabled = false), emptyList(), listOf(estimate)))
    }
}
