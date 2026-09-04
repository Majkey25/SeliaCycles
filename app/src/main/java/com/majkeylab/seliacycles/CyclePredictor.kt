package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class ForecastStatus { RECORDED, ESTIMATED, NOT_EXPECTED, UNAVAILABLE }

data class MonthlyForecast(
    val month: YearMonth,
    val status: ForecastStatus,
    val start: LocalDate?,
    val end: LocalDate?,
    val earliestStart: LocalDate?,
    val latestStart: LocalDate?,
)

data class CyclePrediction(
    val nextPeriodStart: LocalDate?,
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    val periodStarts: List<LocalDate>,
    val uncertaintyDays: Int,
    val earliestPeriodStart: LocalDate?,
    val latestPeriodStart: LocalDate?,
    val estimatedPeriodStarts: List<LocalDate>,
    val monthlyForecasts: List<MonthlyForecast>,
)

object CyclePredictor {
    fun predict(
        bleedingDays: Set<LocalDate>,
        defaultCycleLength: Int,
        defaultPeriodLength: Int,
        referenceDate: LocalDate = LocalDate.now(),
        cycleLengthOverride: Int? = null,
        periodLengthOverride: Int? = null,
        activePeriodStart: LocalDate? = null,
    ): CyclePrediction {
        require(defaultCycleLength in MIN_CYCLE_LENGTH..MAX_CYCLE_LENGTH)
        require(defaultPeriodLength in 1..14)
        require(cycleLengthOverride == null || cycleLengthOverride in MIN_CYCLE_LENGTH..MAX_CYCLE_LENGTH)
        require(periodLengthOverride == null || periodLengthOverride in 1..14)

        val periods = bleedingDays.sorted().fold(mutableListOf<MutableList<LocalDate>>()) { groups, day ->
            val current = groups.lastOrNull()
            if (current == null || ChronoUnit.DAYS.between(current.last(), day) > MAX_PERIOD_GAP_DAYS) {
                groups += mutableListOf(day)
            } else {
                current += day
            }
            groups
        }
        val starts = periods.map { it.first() }
        val rawIntervals = starts.zipWithNext { first, second ->
            ChronoUnit.DAYS.between(first, second).toInt()
        }
        val cycleLengths = robustCycleLengths(rawIntervals, defaultCycleLength)
        val cycleLength = cycleLengthOverride ?: cycleLengths.weightedAverageOr(defaultCycleLength)
        val learnedPeriodLength = periods.filterNot { it.first() == activePeriodStart }.map { period ->
            ChronoUnit.DAYS.between(period.first(), period.last()).toInt() + 1
        }.filter { it in 1..14 }.takeLast(MAX_RECENT_PERIODS).weightedAverageOr(defaultPeriodLength)
        val periodLength = periodLengthOverride ?: learnedPeriodLength
        val uncertainty = when (cycleLengths.size) {
            0, 1 -> DEFAULT_UNCERTAINTY_DAYS
            else -> max(1, cycleLengths.weightedAverageOf { abs(it - cycleLength).toDouble() }.roundToInt())
        }
        val predictions = predictedStarts(
            anchor = starts.lastOrNull(),
            cycleLength = cycleLength,
            referenceMonth = YearMonth.from(referenceDate),
            periodLength = periodLength,
            uncertainty = uncertainty,
        )
        val relevantPredictions = predictions.filterNot { prediction ->
            maxOf(
                prediction.day.plusDays(periodLength.toLong() - 1),
                prediction.uncertainty(uncertainty).second,
            ).isBefore(referenceDate)
        }
        val next = relevantPredictions.firstOrNull()
        val nextWindow = next?.uncertainty(uncertainty)
        val months = (0L..1L).map { YearMonth.from(referenceDate).plusMonths(it) }

        return CyclePrediction(
            nextPeriodStart = next?.day,
            averageCycleLength = cycleLength,
            averagePeriodLength = periodLength,
            periodStarts = starts,
            uncertaintyDays = uncertainty,
            earliestPeriodStart = nextWindow?.first,
            latestPeriodStart = nextWindow?.second,
            estimatedPeriodStarts = relevantPredictions.map(PredictedStart::day),
            monthlyForecasts = months.map { month ->
                monthlyForecast(month, periods, predictions, periodLength, uncertainty)
            },
        )
    }

    private fun robustCycleLengths(rawIntervals: List<Int>, fallback: Int): List<Int> {
        val valid = rawIntervals.filter { it in MIN_CYCLE_LENGTH..MAX_TRACKING_GAP_DAYS }
        val common = valid.filter { it in COMMON_CYCLE_RANGE }.takeLast(MAX_RECENT_CYCLES)
        val direct = valid.filter { it <= MAX_CYCLE_LENGTH }.takeLast(MAX_RECENT_CYCLES)
        val baseline = (common.ifEmpty { direct }).medianOr(fallback)
        val normalized = valid.mapNotNull { interval -> normalizeInterval(interval, baseline) }
            .takeLast(MAX_RECENT_CYCLES)
        if (normalized.size < 3) return normalized

        val median = normalized.medianOr(fallback)
        val medianDeviation = normalized.map { abs(it - median) }.medianOr(0)
        val tolerance = max(MIN_OUTLIER_TOLERANCE, medianDeviation * 3)
        return normalized.filter { abs(it - median) <= tolerance }
    }

    private fun normalizeInterval(interval: Int, baseline: Int): Int? {
        val splitThreshold = max(COMMON_CYCLE_RANGE.last, (baseline * 1.6).roundToInt())
        if (interval <= splitThreshold) return interval.takeIf { it <= MAX_CYCLE_LENGTH }

        val cycles = (interval.toDouble() / baseline).roundToInt().coerceAtLeast(2)
        val expected = baseline * cycles
        val relativeError = abs(interval - expected).toDouble() / expected
        val normalized = (interval.toDouble() / cycles).roundToInt()
        return normalized.takeIf {
            relativeError <= MAX_MULTIPLE_ERROR && it in MIN_CYCLE_LENGTH..MAX_CYCLE_LENGTH
        } ?: interval.takeIf { it <= MAX_CYCLE_LENGTH }
    }

    private fun predictedStarts(
        anchor: LocalDate?,
        cycleLength: Int,
        referenceMonth: YearMonth,
        periodLength: Int,
        uncertainty: Int,
    ): List<PredictedStart> {
        if (anchor == null) return emptyList()
        val firstVisibleDay = referenceMonth.atDay(1)
        val lastVisibleDay = referenceMonth.plusMonths(FORECAST_MONTHS.toLong()).atEndOfMonth()
        val result = mutableListOf<PredictedStart>()
        var cyclesAhead = 1
        var day = anchor.plusDays(cycleLength.toLong())
        while (!day.isAfter(lastVisibleDay)) {
            val prediction = PredictedStart(day, cyclesAhead)
            val visibleEnd = maxOf(
                day.plusDays(periodLength.toLong() - 1),
                prediction.uncertainty(uncertainty).second,
            )
            if (!visibleEnd.isBefore(firstVisibleDay)) result += prediction
            day = day.plusDays(cycleLength.toLong())
            cyclesAhead++
        }
        return result
    }

    private fun monthlyForecast(
        month: YearMonth,
        periods: List<List<LocalDate>>,
        predictions: List<PredictedStart>,
        periodLength: Int,
        uncertainty: Int,
    ): MonthlyForecast {
        periods.lastOrNull { period -> period.any { YearMonth.from(it) == month } }?.let { period ->
            return MonthlyForecast(
                month = month,
                status = ForecastStatus.RECORDED,
                start = period.first(),
                end = period.last(),
                earliestStart = period.first(),
                latestStart = period.first(),
            )
        }
        predictions.firstOrNull { YearMonth.from(it.day) == month }?.let { prediction ->
            val window = prediction.uncertainty(uncertainty)
            return MonthlyForecast(
                month = month,
                status = ForecastStatus.ESTIMATED,
                start = prediction.day,
                end = prediction.day.plusDays(periodLength.toLong() - 1),
                earliestStart = window.first,
                latestStart = window.second,
            )
        }
        val status = if (periods.isEmpty()) ForecastStatus.UNAVAILABLE else ForecastStatus.NOT_EXPECTED
        return MonthlyForecast(month, status, null, null, null, null)
    }

    private fun PredictedStart.uncertainty(baseDays: Int): Pair<LocalDate, LocalDate> {
        val days = ceil(baseDays * sqrt(cyclesAhead.toDouble())).toLong().coerceAtMost(MAX_UNCERTAINTY_DAYS)
        return day.minusDays(days) to day.plusDays(days)
    }

    private fun List<Int>.medianOr(fallback: Int): Int {
        if (isEmpty()) return fallback
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) ((sorted[middle - 1] + sorted[middle]) / 2.0).roundToInt() else sorted[middle]
    }

    private fun List<Int>.weightedAverageOr(fallback: Int): Int =
        if (isEmpty()) fallback else weightedAverageOf(Int::toDouble).roundToInt()

    private inline fun List<Int>.weightedAverageOf(value: (Int) -> Double): Double {
        val weightSum = indices.sumOf { it + 1 }
        return mapIndexed { index, item -> value(item) * (index + 1) }.sum() / weightSum
    }

    private data class PredictedStart(val day: LocalDate, val cyclesAhead: Int)

    private const val MIN_CYCLE_LENGTH = 15
    private const val MAX_CYCLE_LENGTH = 90
    private const val MAX_TRACKING_GAP_DAYS = 180
    private const val MAX_PERIOD_GAP_DAYS = 2L
    private const val MAX_RECENT_CYCLES = 8
    private const val MAX_RECENT_PERIODS = 8
    private const val DEFAULT_UNCERTAINTY_DAYS = 2
    private const val MIN_OUTLIER_TOLERANCE = 4
    private const val MAX_MULTIPLE_ERROR = 0.2
    private const val MAX_UNCERTAINTY_DAYS = 14L
    private const val FORECAST_MONTHS = 12
    private val COMMON_CYCLE_RANGE = 21..45
}
