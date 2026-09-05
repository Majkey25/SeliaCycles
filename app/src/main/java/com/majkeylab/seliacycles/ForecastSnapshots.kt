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
        require(periodStart in DayLog.MIN_DATE..DayLog.MAX_DATE)
        require(earliestStart in DayLog.MIN_DATE..DayLog.MAX_DATE)
        require(latestStart in DayLog.MIN_DATE..DayLog.MAX_DATE)
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
        if (!backup.settings.canPredictPeriods) return emptyList()
        val currentMonth = YearMonth.from(referenceDate)
        val firstMonth = backup.logs.asSequence().filter(DayLog::bleeding).map { YearMonth.from(it.day) }.minOrNull()
            ?: return emptyList()
        return generateSequence(firstMonth) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(currentMonth) }.mapNotNull { month ->
            if (month in existing) return@mapNotNull null
            snapshotForMonth(backup, month, currentMonth)
        }.toList()
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
            cycleLengthOverride = backup.settings.cycleLengthOverride,
            periodLengthOverride = backup.settings.periodLengthOverride,
            activePeriodStart = backup.settings.activePeriodStart,
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

}
