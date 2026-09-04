package com.majkeylab.seliacycles

import java.time.LocalDate

data class TodayDashboardTargets(
    val period: LocalDate?,
    val fertile: LocalDate?,
    val ovulation: LocalDate?,
)

enum class PeriodTiming { UPCOMING, TODAY, LATE }

object TodayDashboard {
    fun periodTiming(distanceDays: Int): PeriodTiming = when {
        distanceDays > 0 -> PeriodTiming.UPCOMING
        distanceDays == 0 -> PeriodTiming.TODAY
        else -> PeriodTiming.LATE
    }

    fun targets(
        insight: DailyCycleInsight,
        fertility: FertilityEstimate? = insight.fertility,
    ): TodayDashboardTargets = TodayDashboardTargets(
        period = insight.nextPeriodStart,
        fertile = fertility?.fertileStart,
        ovulation = fertility?.ovulation,
    )
}
