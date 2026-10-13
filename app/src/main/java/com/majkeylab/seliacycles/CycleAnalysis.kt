package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

data class CycleLengthSample(val start: LocalDate, val days: Int)

data class CycleHistorySample(
    val start: LocalDate,
    val cycleDays: Int,
    val periodDays: Int,
    val ovulation: LocalDate,
    val fertileStart: LocalDate,
    val fertileEnd: LocalDate,
)

data class PredictionAccuracySummary(
    val sampleCount: Int,
    val averageErrorDays: Int,
    val withinWindowCount: Int,
)

object CycleAnalysis {
    fun averageRecordedPeriodDays(
        periodStarts: List<LocalDate>,
        bleedingDays: Set<LocalDate>,
        activePeriodStart: LocalDate?,
        automaticBleedingDays: Set<LocalDate> = emptySet(),
    ): Int? {
        val lengths = periodStarts.takeLast(6).mapNotNull { start ->
            val next = periodStarts.firstOrNull { it > start }
            val end = bleedingDays.filter { it >= start && (next == null || it < next) }.maxOrNull() ?: return@mapNotNull null
            if (activePeriodStart != null && activePeriodStart in start..end) return@mapNotNull null
            if (automaticBleedingDays.any { it in start..end }) return@mapNotNull null
            (ChronoUnit.DAYS.between(start, end).toInt() + 1).takeIf { it in 1..14 }
        }
        return lengths.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    }

    fun recentLengths(periodStarts: List<LocalDate>): List<CycleLengthSample> = periodStarts
        .zipWithNext { start, next -> CycleLengthSample(start, ChronoUnit.DAYS.between(start, next).toInt()) }
        .filter { it.days in 15..90 }
        .takeLast(6)

    fun recentHistory(
        periodStarts: List<LocalDate>,
        bleedingDays: Set<LocalDate>,
        lutealPhaseDays: Int,
    ): List<CycleHistorySample> = periodStarts.zipWithNext().filter { (start, next) ->
        val cycleDays = ChronoUnit.DAYS.between(start, next).toInt()
        cycleDays in 15..90 && cycleDays > lutealPhaseDays
    }.takeLast(6).map { (start, next) ->
        val cycleDays = ChronoUnit.DAYS.between(start, next).toInt()
        val fertility = CycleInsights.fertilityForPeriod(next, lutealPhaseDays)
        CycleHistorySample(
            start = start,
            cycleDays = cycleDays,
            periodDays = bleedingDays.asSequence().filter { it >= start && it < next }
                .maxOfOrNull { ChronoUnit.DAYS.between(start, it).toInt() + 1 } ?: 0,
            ovulation = fertility.ovulation,
            fertileStart = fertility.fertileStart,
            fertileEnd = fertility.fertileEnd,
        )
    }

    fun predictionAccuracy(
        periodStarts: List<LocalDate>,
        snapshots: Map<YearMonth, ForecastSnapshot>,
    ): PredictionAccuracySummary? {
        val samples = snapshots.values.filterNot(ForecastSnapshot::reconstructed)
            .sortedBy(ForecastSnapshot::month).mapNotNull { snapshot ->
            val actual = closestRecordedStart(snapshot, periodStarts) ?: return@mapNotNull null
            AccuracySample(
                actualStart = actual,
                errorDays = abs(ChronoUnit.DAYS.between(snapshot.periodStart, actual).toInt()),
                withinWindow = actual in snapshot.earliestStart..snapshot.latestStart,
            )
        }.distinctBy(AccuracySample::actualStart).takeLast(6)
        if (samples.isEmpty()) return null
        return PredictionAccuracySummary(
            sampleCount = samples.size,
            averageErrorDays = samples.map(AccuracySample::errorDays).average().roundToInt(),
            withinWindowCount = samples.count(AccuracySample::withinWindow),
        )
    }

    internal fun closestRecordedStart(snapshot: ForecastSnapshot, periodStarts: List<LocalDate>): LocalDate? =
        closestRecordedStart(snapshot.periodStart, periodStarts)

    internal fun closestRecordedStart(estimate: LocalDate, periodStarts: List<LocalDate>): LocalDate? =
        periodStarts.minByOrNull { abs(ChronoUnit.DAYS.between(estimate, it)) }
            ?.takeIf { abs(ChronoUnit.DAYS.between(estimate, it)) <= MAX_ESTIMATE_MATCH_DAYS }

    internal fun closestSnapshot(actualStart: LocalDate, snapshots: Collection<ForecastSnapshot>): ForecastSnapshot? =
        snapshots.minByOrNull { abs(ChronoUnit.DAYS.between(it.periodStart, actualStart)) }
            ?.takeIf { abs(ChronoUnit.DAYS.between(it.periodStart, actualStart)) <= MAX_ESTIMATE_MATCH_DAYS }

    private data class AccuracySample(val actualStart: LocalDate, val errorDays: Int, val withinWindow: Boolean)

    private const val MAX_ESTIMATE_MATCH_DAYS = 14L
}
