package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth

data class ForecastSnapshot(
    val month: YearMonth,
    val periodStart: LocalDate,
    val earliestStart: LocalDate,
    val latestStart: LocalDate,
    val periodLength: Int,
    val reconstructed: Boolean,
) {
    init {
        require(YearMonth.from(periodStart) == month)
        require(!earliestStart.isAfter(periodStart))
        require(!latestStart.isBefore(periodStart))
        require(periodLength in 1..14)
    }
}

object ForecastSnapshotPlanner {
    fun missingSnapshots(
        backup: CycleBackup,
        existing: Map<YearMonth, ForecastSnapshot>,
        referenceDate: LocalDate = LocalDate.now(),
    ): List<ForecastSnapshot> {
        val currentMonth = YearMonth.from(referenceDate)
        return (HISTORY_MONTHS downTo 0L).mapNotNull { monthsAgo ->
            val month = currentMonth.minusMonths(monthsAgo)
            if (month in existing) return@mapNotNull null
            snapshotForMonth(backup, month, currentMonth)
        }
    }

    private fun snapshotForMonth(
        backup: CycleBackup,
        month: YearMonth,
        currentMonth: YearMonth,
    ): ForecastSnapshot? {
        val firstDay = month.atDay(1)
        val earlierBleeding = backup.logs.asSequence()
            .filter { it.bleeding && it.day.isBefore(firstDay) }
            .map(DayLog::day)
            .toSet()
        if (earlierBleeding.isEmpty()) return null
        val forecast = CyclePredictor.predict(
            bleedingDays = earlierBleeding,
            defaultCycleLength = backup.settings.cycleLength,
            defaultPeriodLength = backup.settings.periodLength,
            referenceDate = firstDay,
        ).monthlyForecasts.first { it.month == month }
        if (forecast.status != ForecastStatus.ESTIMATED) return null
        return ForecastSnapshot(
            month = month,
            periodStart = requireNotNull(forecast.start),
            earliestStart = requireNotNull(forecast.earliestStart),
            latestStart = requireNotNull(forecast.latestStart),
            periodLength = requireNotNull(forecast.end).toEpochDay().minus(forecast.start.toEpochDay()).toInt() + 1,
            reconstructed = month < currentMonth || backup.logs.any { it.bleeding && YearMonth.from(it.day) == month },
        )
    }

    private const val HISTORY_MONTHS = 12L
}
