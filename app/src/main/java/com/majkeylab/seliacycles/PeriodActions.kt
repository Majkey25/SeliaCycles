package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class TodayPrimaryAction { START_PERIOD, END_PERIOD, OPEN_LOG }

object PeriodActions {
    fun suggestedStart(day: LocalDate, settings: AppSettings, logs: List<DayLog>, estimates: List<PeriodEstimate>): LocalDate? {
        settings.activePeriodStart?.takeIf { ChronoUnit.DAYS.between(it, day) in 0 until MAX_PERIOD_DAYS }?.let { return it }
        periodContaining(day, logs)?.let { return it.first }
        if (!settings.canPredictPeriods) return null
        return estimates.filter { day >= it.start && day < it.endExclusive }.maxOfOrNull(PeriodEstimate::start)
    }

    fun removeFutureBleeding(logs: List<DayLog>, today: LocalDate): List<DayLog> = logs.mapNotNull { log ->
        if (!log.day.isAfter(today) || !log.bleeding || log.automaticBleeding) return@mapNotNull log
        log.copy(bleeding = false, flow = Flow.NONE).takeUnless(DayLog::isEmpty)
    }

    fun todayAction(settings: AppSettings, day: LocalDate): TodayPrimaryAction = when {
        !settings.canPredictPeriods -> TodayPrimaryAction.OPEN_LOG
        settings.activePeriodStart?.let { ChronoUnit.DAYS.between(it, day) in 0..13 } == true ->
            TodayPrimaryAction.END_PERIOD
        else -> TodayPrimaryAction.START_PERIOD
    }

    fun start(day: LocalDate, logs: List<DayLog>, periodLength: Int = 5): List<DayLog> {
        require(periodLength in 1..MAX_PERIOD_DAYS)
        val byDay = logs.associateByTo(mutableMapOf(), DayLog::day)
        byDay[day] = (byDay[day] ?: DayLog(day)).withBleeding()
        (1L until periodLength.toLong()).forEach { offset ->
            val date = day.plusDays(offset)
            val existing = byDay[date] ?: DayLog(date)
            if (!existing.confirmedBleeding) byDay[date] = existing.withBleeding().copy(automaticBleeding = true)
        }
        return byDay.values.sortedBy(DayLog::day)
    }

    fun end(day: LocalDate, logs: List<DayLog>, suggestedStart: LocalDate?): List<DayLog> {
        val period = periodContaining(day, logs)
        val start = period?.first ?: suggestedStart?.takeIf {
            !it.isAfter(day) && ChronoUnit.DAYS.between(it, day) in 0..13
        } ?: return logs
        val byDay = logs.associateByTo(mutableMapOf(), DayLog::day)
        generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(day) }.forEach { date ->
            byDay[date] = (byDay[date] ?: DayLog(date)).withBleeding()
        }
        period?.second?.takeIf { it.isAfter(day) }?.let { oldEnd ->
            generateSequence(day.plusDays(1)) { it.plusDays(1) }.takeWhile { !it.isAfter(oldEnd) }.forEach { date ->
                byDay[date]?.copy(bleeding = false, flow = Flow.NONE, automaticBleeding = false)?.let { updated ->
                    if (updated.isEmpty) byDay.remove(date) else byDay[date] = updated
                }
            }
        }
        return byDay.values.sortedBy(DayLog::day)
    }

    fun periodDays(day: LocalDate, logs: List<DayLog>): Set<LocalDate> {
        val period = periodContaining(day, logs) ?: return emptySet()
        return logs.asSequence().filter { it.bleeding && it.day in period.first..period.second }
            .mapTo(mutableSetOf(), DayLog::day)
    }

    fun suggestedDays(day: LocalDate, logs: List<DayLog>, periodLength: Int, today: LocalDate): Set<LocalDate> {
        require(periodLength in 1..MAX_PERIOD_DAYS)
        require(day in DayLog.MIN_DATE..DayLog.MAX_DATE && (day <= today || logs.any { it.day == day && it.automaticBleeding }))
        return periodDays(day, logs).ifEmpty {
            (0L until periodLength.toLong()).map(day::plusDays).toSet()
        }
    }

    fun isValidSelection(days: Set<LocalDate>, today: LocalDate): Boolean =
        days.size <= MAX_PERIOD_DAYS && days.all { it in DayLog.MIN_DATE..minOf(today.plusDays(13), DayLog.MAX_DATE) } &&
            (days.isEmpty() || days.min() <= today) &&
            (days.isEmpty() || ChronoUnit.DAYS.between(days.min(), days.max()) < MAX_PERIOD_DAYS)

    fun replace(
        day: LocalDate,
        selectedDays: Set<LocalDate>,
        logs: List<DayLog>,
        today: LocalDate,
    ): List<DayLog> {
        require(day in DayLog.MIN_DATE..DayLog.MAX_DATE && (day <= today || logs.any { it.day == day && it.automaticBleeding }))
        require(isValidSelection(selectedDays, today))
        val byDay = logs.associateByTo(mutableMapOf(), DayLog::day)
        (periodDays(day, logs) + selectedDays).forEach { date ->
            if (date in selectedDays) {
                byDay[date] = (byDay[date] ?: DayLog(date)).withBleeding().copy(automaticBleeding = date > today)
            } else {
                byDay[date]?.copy(bleeding = false, flow = Flow.NONE, automaticBleeding = false)?.let { updated ->
                    if (updated.isEmpty) byDay.remove(date) else byDay[date] = updated
                }
            }
        }
        return byDay.values.sortedBy(DayLog::day)
    }

    private fun DayLog.withBleeding(): DayLog = copy(
        bleeding = true,
        automaticBleeding = false,
        flow = flow.takeUnless { it == Flow.NONE } ?: Flow.UNKNOWN,
    )

    private fun periodContaining(day: LocalDate, logs: List<DayLog>): Pair<LocalDate, LocalDate>? {
        val groups = logs.asSequence().filter(DayLog::bleeding).map(DayLog::day).sorted()
            .fold(mutableListOf<MutableList<LocalDate>>()) { periods, date ->
                val current = periods.lastOrNull()
                if (current == null || ChronoUnit.DAYS.between(current.last(), date) > MAX_GAP_DAYS) {
                    periods += mutableListOf(date)
                } else {
                    current += date
                }
                periods
            }
        return groups.firstOrNull { day in it.first()..it.last() }?.let { it.first() to it.last() }
    }

    private const val MAX_GAP_DAYS = 2L
    private const val MAX_PERIOD_DAYS = 14
}
