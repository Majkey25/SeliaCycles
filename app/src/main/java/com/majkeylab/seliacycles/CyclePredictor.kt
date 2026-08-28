package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class CyclePrediction(
    val nextPeriodStart: LocalDate?,
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    val periodStarts: List<LocalDate>,
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
        val cycleLength = starts.zipWithNext { first, second ->
            ChronoUnit.DAYS.between(first, second).toInt()
        }.filter { it in 15..90 }.takeLast(6).averageOr(defaultCycleLength)
        val periodLength = periods.map { period ->
            ChronoUnit.DAYS.between(period.first(), period.last()).toInt() + 1
        }.filter { it in 1..14 }.takeLast(6).averageOr(defaultPeriodLength)

        return CyclePrediction(
            nextPeriodStart = starts.lastOrNull()?.plusDays(cycleLength.toLong()),
            averageCycleLength = cycleLength,
            averagePeriodLength = periodLength,
            periodStarts = starts,
        )
    }

    private fun List<Int>.averageOr(fallback: Int): Int =
        if (isEmpty()) fallback else average().roundToInt()
}
