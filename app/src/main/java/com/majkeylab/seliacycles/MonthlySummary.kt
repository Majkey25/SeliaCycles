package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth

internal data class MonthlySummary(
    val month: YearMonth,
    val recordedDays: Set<LocalDate>,
    val detailDays: Set<LocalDate>,
    val recordedRuns: List<ClosedRange<LocalDate>>,
    val estimates: List<PeriodEstimate>,
    val fertility: List<FertilityEstimate>,
    val moodCounts: Map<Mood, Int>,
    val energyCounts: Map<WellbeingLevel, Int>,
    val painDays: Int,
    val sleepAverage: Double?,
) {
    companion object {
        fun create(
            month: YearMonth,
            logs: List<DayLog>,
            estimates: List<PeriodEstimate>,
            fertility: List<FertilityEstimate>,
            referenceDate: LocalDate,
        ): MonthlySummary {
            val monthLogs = logs.filter { YearMonth.from(it.day) == month }
            val observations = monthLogs.filter { it.day <= referenceDate }
            val recorded = observations.filter(DayLog::bleeding).mapTo(sortedSetOf(), DayLog::day)
            val runs = mutableListOf<ClosedRange<LocalDate>>()
            recorded.forEach { day ->
                val last = runs.lastOrNull()
                if (last != null && last.endInclusive.plusDays(1) == day) {
                    runs[runs.lastIndex] = last.start..day
                } else {
                    runs += day..day
                }
            }
            val sleep = observations.mapNotNull(DayLog::sleepHours)
            return MonthlySummary(
                month = month,
                recordedDays = recorded,
                detailDays = monthLogs.filter(DayLog::hasCalendarMarker).mapTo(sortedSetOf(), DayLog::day),
                recordedRuns = runs,
                estimates = estimates.filter { it.start <= month.atEndOfMonth() && it.endExclusive > month.atDay(1) },
                fertility = fertility.filter { it.fertileStart <= month.atEndOfMonth() && it.fertileEnd >= month.atDay(1) },
                moodCounts = observations.mapNotNull(DayLog::mood).groupingBy { it }.eachCount(),
                energyCounts = observations.mapNotNull(DayLog::energy).groupingBy { it }.eachCount(),
                painDays = observations.count { (it.painLevel ?: 0) > 0 },
                sleepAverage = sleep.takeIf { it.isNotEmpty() }?.average(),
            )
        }
    }
}
