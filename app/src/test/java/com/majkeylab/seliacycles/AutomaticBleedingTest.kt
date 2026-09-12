package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutomaticBleedingTest {
    private val start = LocalDate.of(2026, 9, 12)

    @Test fun `automatic range survives normalization and backup without training period duration`() {
        val history = (0L..2L).map { DayLog(start.minusDays(28).plusDays(it), bleeding = true, flow = Flow.LIGHT) }
        val logs = PeriodActions.start(start, history, 6)
        assertEquals(logs, PeriodActions.removeFutureBleeding(logs, start))
        val backup = CycleBackup(logs)
        assertEquals(3, CycleInsights.prediction(backup, start).averagePeriodLength)
        assertEquals(start.plusDays(28), CycleInsights.prediction(backup, start).nextPeriodStart)
        val transfer = SeliaTransfer(backup, emptyList())
        assertEquals(transfer, SeliaBackupCodec.decode(SeliaBackupCodec.encode(transfer)))
        assertEquals(2, MyCalendarExportMapper.periodRows(logs).size)
        assertEquals(1, MyCalendarExportMapper.periodRows(logs).last().periodValue)
    }

    @Test fun `end confirms only completed days and keeps optional information`() {
        val note = DayLog(start.plusDays(5), note = "keep")
        val ended = PeriodActions.end(start.plusDays(2), PeriodActions.start(start, listOf(note), 6), start)
        assertEquals((0L..2L).map(start::plusDays), ended.filter(DayLog::confirmedBleeding).map(DayLog::day))
        assertTrue(ended.none(DayLog::automaticBleeding))
        assertEquals(note, ended.last())
    }

    @Test fun `manual confirmation wins when merging an automatic day`() {
        val automatic = DayLog(start, bleeding = true, flow = Flow.UNKNOWN, automaticBleeding = true)
        val confirmed = automatic.copy(automaticBleeding = false, flow = Flow.HEAVY)
        assertEquals(confirmed, mergeDayLogs(automatic, confirmed))
        assertEquals(confirmed, mergeDayLogs(confirmed, automatic))
    }
}
