package com.majkeylab.seliacycles

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object CalendarPaging {
    private val firstMonth = YearMonth.from(DayLog.MIN_DATE)
    private val lastMonth = YearMonth.from(DayLog.MAX_DATE)

    val pageCount: Int = ChronoUnit.MONTHS.between(firstMonth, lastMonth).toInt() + 1

    fun pageFor(month: YearMonth): Int {
        require(month in firstMonth..lastMonth)
        return ChronoUnit.MONTHS.between(firstMonth, month).toInt()
    }

    fun monthFor(page: Int): YearMonth {
        require(page in 0 until pageCount)
        return firstMonth.plusMonths(page.toLong())
    }

    fun gridDays(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
        val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val first = month.atDay(1).minusDays(leading.toLong())
        val size = ((leading + month.lengthOfMonth() + 6) / 7) * 7
        return List(size) { first.plusDays(it.toLong()) }
    }

    fun periodEditorDays(base: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> {
        val leading = (base.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val first = maxOf(DayLog.MIN_DATE, base.minusDays(leading.toLong()).minusWeeks(1))
        return List(28) { first.plusDays(it.toLong()) }
    }
}

internal enum class CalendarPeriodLayer { NONE, RECORDED, PREDICTED }

internal data class CalendarDayTracks(
    val period: CalendarPeriodLayer,
    val fertile: Boolean,
    val ovulation: Boolean,
    val predictedOverlap: Boolean,
)

internal fun calendarDayTracks(
    day: LocalDate,
    recorded: Set<LocalDate>,
    predicted: Set<LocalDate>,
    fertile: Set<LocalDate>,
    ovulation: Set<LocalDate>,
): CalendarDayTracks = CalendarDayTracks(
    period = when {
        day in recorded -> CalendarPeriodLayer.RECORDED
        day in predicted -> CalendarPeriodLayer.PREDICTED
        else -> CalendarPeriodLayer.NONE
    },
    fertile = day in fertile,
    ovulation = day in ovulation,
    predictedOverlap = day in recorded && day in predicted,
)

/** Shared calendar markings for the app and the home-screen widget. */
internal class CalendarTracks(content: CycleContent, showFertility: Boolean) {
    private val recorded = content.backup.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day)
    private val recordedMonths = content.prediction.periodStarts.mapTo(mutableSetOf(), YearMonth::from)
    private val predicted = (content.periodEstimates.flatMap { estimate ->
        generateSequence(estimate.start) { it.plusDays(1) }.takeWhile { it < estimate.endExclusive }.toList()
    } + content.forecastSnapshots.values.filter { it.month in recordedMonths }.flatMap { snapshot ->
        (0L until snapshot.periodLength.toLong()).map(snapshot.periodStart::plusDays)
    }).toSet()
    private val fertility = if (showFertility) CycleInsights.fertilityEstimates(
        content.backup, content.forecastSnapshots, content.referenceDate,
    ) else emptyList()
    private val fertile = fertility.flatMap { estimate ->
        generateSequence(estimate.fertileStart) { it.plusDays(1) }.takeWhile { it <= estimate.fertileEnd }.toList()
    }.toSet()
    private val ovulation = fertility.mapTo(mutableSetOf(), FertilityEstimate::ovulation)

    fun forDay(day: LocalDate): CalendarDayTracks = calendarDayTracks(day, recorded, predicted, fertile, ovulation)
}
