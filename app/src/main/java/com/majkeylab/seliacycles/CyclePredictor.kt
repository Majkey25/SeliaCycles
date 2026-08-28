package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class CyclePrediction(
    val nextPeriodStart: LocalDate?,
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    val periodStarts: List<LocalDate>,
    val uncertaintyDays: Int,
    val earliestPeriodStart: LocalDate?,
    val latestPeriodStart: LocalDate?,
)

object CyclePredictor {
    fun predict(
        bleedingDays: Set<LocalDate>,
        defaultCycleLength: Int,
        defaultPeriodLength: Int,
    ): CyclePrediction {
        require(defaultCycleLength in 15..90)
        require(defaultPeriodLength in 1..14)

        val periods = bleedingDays.sorted().fold(mutableListOf<MutableList<LocalDate>>()) { groups, day ->
            val current = groups.lastOrNull()
            if (current == null || ChronoUnit.DAYS.between(current.last(), day) > 2) {
                groups += mutableListOf(day)
            } else {
                current += day
            }
            groups
        }
        val starts = periods.map { it.first() }
        val cycleLengths = starts.zipWithNext { first, second ->
            ChronoUnit.DAYS.between(first, second).toInt()
        }.filter { it in 15..90 }.takeLast(6)
        val cycleLength = cycleLengths.weightedAverageOr(defaultCycleLength)
        val periodLength = periods.map { period ->
            ChronoUnit.DAYS.between(period.first(), period.last()).toInt() + 1
        }.filter { it in 1..14 }.takeLast(6).weightedAverageOr(defaultPeriodLength)
        val uncertainty = when (cycleLengths.size) {
            0, 1 -> 2
            else -> max(1, cycleLengths.weightedAverageOf { abs(it - cycleLength).toDouble() }.roundToInt())
        }
        val next = starts.lastOrNull()?.plusDays(cycleLength.toLong())

        return CyclePrediction(
            nextPeriodStart = next,
            averageCycleLength = cycleLength,
            averagePeriodLength = periodLength,
            periodStarts = starts,
            uncertaintyDays = uncertainty,
            earliestPeriodStart = next?.minusDays(uncertainty.toLong()),
            latestPeriodStart = next?.plusDays(uncertainty.toLong()),
        )
    }

    private fun List<Int>.weightedAverageOr(fallback: Int): Int =
        if (isEmpty()) fallback else weightedAverageOf(Int::toDouble).roundToInt()

    private inline fun List<Int>.weightedAverageOf(value: (Int) -> Double): Double {
        val weightSum = indices.sumOf { it + 1 }
        return mapIndexed { index, item -> value(item) * (index + 1) }.sum() / weightSum
    }
}
