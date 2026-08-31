package com.majkeylab.seliacycles

import java.time.LocalDate

data class TodayDashboardTargets(
    val period: LocalDate?,
    val fertile: LocalDate?,
    val ovulation: LocalDate?,
)

object TodayDashboard {
    fun targets(insight: DailyCycleInsight): TodayDashboardTargets = TodayDashboardTargets(
        period = insight.nextPeriodStart,
        fertile = insight.fertility?.fertileStart,
        ovulation = insight.fertility?.ovulation,
    )
}
