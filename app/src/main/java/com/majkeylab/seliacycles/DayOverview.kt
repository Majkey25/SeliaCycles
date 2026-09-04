package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class EstimateAccuracy { NO_REALITY, EXACT, EARLY, LATE }

data class DayEstimateComparison(
    val snapshot: ForecastSnapshot,
    val actualStart: LocalDate?,
    val differenceDays: Int?,
    val accuracy: EstimateAccuracy,
)

object DayOverview {
    fun showQuickPeriodEntry(day: LocalDate, today: LocalDate): Boolean = day == today

    fun compare(
        day: LocalDate,
        backup: CycleBackup,
        snapshots: Map<YearMonth, ForecastSnapshot>,
    ): DayEstimateComparison? {
        val starts = periodStarts(backup.logs)
        val recordedStartForDay = if (backup.logs.any { it.day == day && it.bleeding }) {
            starts.lastOrNull { start -> !start.isAfter(day) && ChronoUnit.DAYS.between(start, day) in 0..13 }
        } else null
        val snapshot = recordedStartForDay?.let { CycleAnalysis.closestSnapshot(it, snapshots.values) }
            ?: snapshots[YearMonth.from(day)]
            ?: return null
        val actual = CycleAnalysis.closestRecordedStart(snapshot, starts)
        val estimatedPeriod = day >= snapshot.periodStart &&
            day < snapshot.periodStart.plusDays(snapshot.periodLength.toLong())
        val recordedPeriod = recordedStartForDay != null && recordedStartForDay == actual
        if (!estimatedPeriod && !recordedPeriod) return null
        val difference = actual?.let { ChronoUnit.DAYS.between(snapshot.periodStart, it).toInt() }
        return DayEstimateComparison(
            snapshot = snapshot,
            actualStart = actual,
            differenceDays = difference,
            accuracy = when {
                difference == null -> EstimateAccuracy.NO_REALITY
                difference == 0 -> EstimateAccuracy.EXACT
                difference < 0 -> EstimateAccuracy.EARLY
                else -> EstimateAccuracy.LATE
            },
        )
    }

    private fun periodStarts(logs: List<DayLog>): List<LocalDate> = buildList {
        var previous: LocalDate? = null
        logs.asSequence().filter(DayLog::bleeding).map(DayLog::day).sorted().forEach { day ->
            if (previous == null || ChronoUnit.DAYS.between(previous, day) > MAX_GAP_DAYS) add(day)
            previous = day
        }
    }

    private const val MAX_GAP_DAYS = 2L
}
