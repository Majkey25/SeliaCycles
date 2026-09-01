package com.majkeylab.seliacycles

import java.time.LocalDate

data class TodayDashboardTargets(
    val period: LocalDate?,
    val fertile: LocalDate?,
    val ovulation: LocalDate?,
)

object TodayDashboard {
    fun targets(
        insight: DailyCycleInsight,
        fertility: FertilityEstimate? = insight.fertility,
    ): TodayDashboardTargets = TodayDashboardTargets(
        period = insight.nextPeriodStart,
        fertile = fertility?.fertileStart,
        ovulation = fertility?.ovulation,
    )
}
