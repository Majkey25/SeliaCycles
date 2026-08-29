package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class MirrorEventKind { RECORDED, ESTIMATED, FERTILE, OVULATION }

data class MirrorEvent(
    val kind: MirrorEventKind,
    val start: LocalDate,
    val endExclusive: LocalDate,
) {
    init {
        require(endExclusive.isAfter(start))
    }

    val key: String
        get() = "${kind.name.lowercase()}/$start"
}

data class StoredMirrorEvent(val id: Long, val key: String)

sealed interface MirrorMutation {
    data class Insert(val event: MirrorEvent) : MirrorMutation
    data class Update(val id: Long, val event: MirrorEvent) : MirrorMutation
    data class Delete(val id: Long) : MirrorMutation
}

object CalendarMirrorDiff {
    fun plan(desired: List<MirrorEvent>, existing: List<StoredMirrorEvent>): List<MirrorMutation> {
        require(desired.map(MirrorEvent::key).distinct().size == desired.size)
        val remaining = existing.groupBy(StoredMirrorEvent::key).toMutableMap()
        return buildList {
            desired.forEach { event ->
                val matches = remaining.remove(event.key).orEmpty()
                if (matches.isEmpty()) add(MirrorMutation.Insert(event)) else {
                    add(MirrorMutation.Update(matches.first().id, event))
                    matches.drop(1).forEach { add(MirrorMutation.Delete(it.id)) }
                }
            }
            remaining.values.flatten().forEach { add(MirrorMutation.Delete(it.id)) }
        }
    }
}

object CalendarMirrorPlanner {
    fun plan(
        backup: CycleBackup,
        snapshots: Map<java.time.YearMonth, ForecastSnapshot>,
        referenceDate: LocalDate = LocalDate.now(),
    ): List<MirrorEvent> {
        val firstDay = referenceDate.minusMonths(HISTORY_MONTHS).withDayOfMonth(1)
        val lastDay = referenceDate.plusMonths(FORECAST_MONTHS).with(TemporalAdjusters.lastDayOfMonth())
        val bleedingDays = backup.logs.filter(DayLog::bleeding).map(DayLog::day).sorted()
        val periods = bleedingDays.fold(mutableListOf<MutableList<LocalDate>>()) { groups, day ->
            val current = groups.lastOrNull()
            if (current == null || ChronoUnit.DAYS.between(current.last(), day) > MAX_PERIOD_GAP_DAYS) {
                groups += mutableListOf(day)
            } else {
                current += day
            }
            groups
        }
        val recorded = periods.filter { it.last() >= firstDay && it.first() <= lastDay }.map {
            MirrorEvent(MirrorEventKind.RECORDED, it.first(), it.last().plusDays(1))
        }
        val estimates = CycleInsights.periodEstimates(backup, snapshots, referenceDate)
            .filter { it.endExclusive >= firstDay && it.start <= lastDay }
        val estimated = estimates.map { MirrorEvent(MirrorEventKind.ESTIMATED, it.start, it.endExclusive) }
        val fertility = estimates.flatMap { estimate ->
            val value = CycleInsights.fertilityForPeriod(estimate.start)
            listOf(
                MirrorEvent(MirrorEventKind.FERTILE, value.fertileStart, value.fertileEnd.plusDays(1)),
                MirrorEvent(MirrorEventKind.OVULATION, value.ovulation, value.ovulation.plusDays(1)),
            )
        }.filter { it.endExclusive >= firstDay && it.start <= lastDay }
        return (recorded + estimated + fertility).sortedWith(compareBy(MirrorEvent::start, MirrorEvent::kind))
    }

    private const val HISTORY_MONTHS = 12L
    private const val FORECAST_MONTHS = 12L
    private const val MAX_PERIOD_GAP_DAYS = 2L
}
