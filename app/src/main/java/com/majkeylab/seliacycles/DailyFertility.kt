package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth

// These levels describe calendar estimates, not a probability of pregnancy.
enum class DailyFertilityLevel { UNAVAILABLE, OUTSIDE_ESTIMATE, POSSIBLE, FERTILE_WINDOW, ESTIMATED_OVULATION }

object DailyFertility {
    fun forDate(
        date: LocalDate,
        backup: CycleBackup,
        prediction: CyclePrediction,
        estimates: List<PeriodEstimate>,
        referenceDate: LocalDate,
    ): DailyFertilityLevel {
        if (date !in DayLog.MIN_DATE..DayLog.MAX_DATE || referenceDate !in DayLog.MIN_DATE..DayLog.MAX_DATE ||
            !CycleInsights.canEstimateFertility(backup, prediction)
        ) return DailyFertilityLevel.UNAVAILABLE

        val validEstimates = estimates.filter { isValid(it, prediction) }
        val estimatesByStart = validEstimates.associateBy(PeriodEstimate::start)
        val recorded = prediction.periodStarts.filterTo(mutableSetOf()) { it > referenceDate }
        val day = date.toEpochDay()
        return CycleInsights.fertilityEstimates(backup, prediction, validEstimates, referenceDate).map { central ->
            val estimate = estimatesByStart[central.periodStart].takeUnless { central.periodStart in recorded }
            val start = central.periodStart.toEpochDay()
            val earliest = estimate?.earliestStart?.toEpochDay() ?: start
            val latest = estimate?.latestStart?.toEpochDay() ?: start
            val possibleStart = central.fertileStart.toEpochDay() - (start - earliest)
            val possibleEnd = central.fertileEnd.toEpochDay() + (latest - start)
            val coverageStart = minOf(possibleStart, earliest - prediction.averageCycleLength)
            val coverageEnd = maxOf(possibleEnd, latest,
                estimate?.endExclusive?.toEpochDay()?.minus(1) ?: (start + prediction.averagePeriodLength - 1))
            when {
                date == central.ovulation -> DailyFertilityLevel.ESTIMATED_OVULATION
                date in central.fertileStart..central.fertileEnd -> DailyFertilityLevel.FERTILE_WINDOW
                day in possibleStart..possibleEnd -> DailyFertilityLevel.POSSIBLE
                day in coverageStart..coverageEnd -> DailyFertilityLevel.OUTSIDE_ESTIMATE
                else -> DailyFertilityLevel.UNAVAILABLE
            }
        }.maxByOrNull(DailyFertilityLevel::ordinal) ?: DailyFertilityLevel.UNAVAILABLE
    }

    fun forMonth(
        month: YearMonth,
        backup: CycleBackup,
        prediction: CyclePrediction,
        estimates: List<PeriodEstimate>,
        referenceDate: LocalDate,
    ): Map<LocalDate, DailyFertilityLevel> = (1..month.lengthOfMonth()).associate { day ->
        val date = month.atDay(day)
        date to forDate(date, backup, prediction, estimates, referenceDate)
    }

    private fun isValid(estimate: PeriodEstimate, prediction: CyclePrediction): Boolean {
        val earliest = estimate.earliestStart ?: estimate.start
        val latest = estimate.latestStart ?: estimate.start
        val supportedDates = listOf(estimate.start, earliest, latest).all { it in DayLog.MIN_DATE..DayLog.MAX_DATE }
        val generatedBoundary = !supportedDates && prediction.periodStarts.isNotEmpty() &&
            estimate.origin == EstimateOrigin.CURRENT && estimate.start in prediction.estimatedPeriodStarts &&
            prediction.uncertaintyWindow(estimate.start) == (earliest to latest)
        return (supportedDates || generatedBoundary) && earliest <= estimate.start && latest >= estimate.start &&
            estimate.endExclusive > estimate.start &&
            estimate.endExclusive.toEpochDay() - estimate.start.toEpochDay() <= 14
    }
}
