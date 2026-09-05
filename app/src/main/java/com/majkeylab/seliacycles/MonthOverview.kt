package com.majkeylab.seliacycles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
internal fun MonthOverview(state: AppState, month: YearMonth, locale: Locale, onDayClick: (LocalDate) -> Unit) {
    val summary = remember(state.backup, state.forecastSnapshots, state.referenceDate, month) {
        MonthlySummary.create(
            month, state.backup.logs, state.periodEstimates,
            CycleInsights.fertilityEstimates(state.backup, state.forecastSnapshots, state.referenceDate),
            state.referenceDate,
        )
    }
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val periodColor = calendarPeriodRgb(state.backup.settings.palette, state.backup.settings.customPalette).color()
    val entryColor = calendarEntryRgb(state.backup.settings.palette, state.backup.settings.customPalette).color()
    val saved = state.forecastSnapshots[month]
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MonthCount(summary.recordedDays.size, stringResource(R.string.month_bleeding_days), Icons.Outlined.WaterDrop, Modifier.weight(1f))
            MonthCount(summary.detailDays.size, stringResource(R.string.month_detail_days), Icons.Outlined.Edit, Modifier.weight(1f))
        }
        Text(stringResource(R.string.month_timeline), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (summary.recordedDays.isNotEmpty()) {
            MonthTrack(month, summary.recordedDays, stringResource(R.string.recorded_legend), periodColor)
        }
        val estimatedDays = summary.estimates.flatMap { estimate ->
            (1..month.lengthOfMonth()).map(month::atDay).filter { it >= estimate.start && it < estimate.endExclusive }
        }.toSet()
        if (estimatedDays.isNotEmpty()) {
            MonthTrack(month, estimatedDays, stringResource(R.string.predicted_legend), calendarPredictedPeriodColor(periodColor))
        }
        saved?.let { snapshot ->
            val savedDays = (1..month.lengthOfMonth()).map(month::atDay).filter {
                it >= snapshot.periodStart && it < snapshot.periodStart.plusDays(snapshot.periodLength.toLong())
            }.toSet()
            if (savedDays != estimatedDays) {
                MonthTrack(month, savedDays,
                    stringResource(if (snapshot.reconstructed) R.string.month_reconstructed_prediction else R.string.month_saved_prediction),
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
            }
        }
        if (summary.fertility.isNotEmpty()) {
            val fertileDays = (1..month.lengthOfMonth()).map(month::atDay)
                .filter { day -> summary.fertility.any { day in it.fertileStart..it.fertileEnd } }.toSet()
            MonthTrack(month, fertileDays, stringResource(R.string.fertile_legend), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
        }
        if (summary.detailDays.isNotEmpty()) {
            MonthTrack(month, summary.detailDays, stringResource(R.string.recorded_values), entryColor)
        }
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(1, 15, month.lengthOfMonth()).forEach { Text(it.toString(), style = MaterialTheme.typography.labelSmall) }
        }
        Text(stringResource(R.string.month_timeline_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        summary.recordedRuns.forEach { run ->
            MonthDateRow(Icons.Outlined.WaterDrop, stringResource(R.string.recorded_legend),
                dateRange(run.start, run.endInclusive, format), onClick = { onDayClick(run.start) })
        }
        summary.estimates.filterNot { it.origin != EstimateOrigin.CURRENT && it.start == saved?.periodStart }.forEach { estimate ->
            val origin = when (estimate.origin) {
                EstimateOrigin.CURRENT -> R.string.predicted_legend
                EstimateOrigin.SAVED -> R.string.month_saved_prediction
                EstimateOrigin.RECONSTRUCTED -> R.string.month_reconstructed_prediction
            }
            MonthDateRow(Icons.Outlined.EventRepeat, stringResource(origin),
                dateRange(estimate.start, estimate.endExclusive.minusDays(1), format),
                supporting = estimate.earliestStart?.let { earliest ->
                    estimate.latestStart?.let { latest -> stringResource(R.string.month_start_window, dateRange(earliest, latest, format)) }
                }, onClick = { onDayClick(estimate.start.coerceIn(DayLog.MIN_DATE, DayLog.MAX_DATE)) })
        }
        summary.fertility.forEach { fertility ->
            MonthDateRow(Icons.Outlined.Spa, stringResource(R.string.fertile_legend),
                dateRange(fertility.fertileStart, fertility.fertileEnd, format),
                supporting = stringResource(R.string.estimated_ovulation, fertility.ovulation.format(format)),
                onClick = { onDayClick(fertility.ovulation.coerceIn(DayLog.MIN_DATE, DayLog.MAX_DATE)) })
        }
        if (saved != null) {
            HorizontalDivider()
            val actual = CycleAnalysis.closestRecordedStart(saved, state.prediction.periodStarts)
            MonthDateRow(Icons.Outlined.History,
                stringResource(if (saved.reconstructed) R.string.month_reconstructed_prediction else R.string.month_saved_prediction),
                dateRange(saved.periodStart, saved.periodStart.plusDays(saved.periodLength.toLong() - 1), format),
                onClick = { onDayClick(saved.periodStart) })
            if (saved.reconstructed) {
                Text(stringResource(R.string.month_reconstruction_notice), style = MaterialTheme.typography.bodySmall)
            } else if (actual != null) {
                val difference = ChronoUnit.DAYS.between(saved.periodStart, actual).toInt()
                Text(if (difference == 0) stringResource(R.string.month_prediction_matched) else {
                    stringResource(if (difference < 0) R.string.month_prediction_earlier else R.string.month_prediction_later,
                        pluralStringResource(R.plurals.days_value, kotlin.math.abs(difference), kotlin.math.abs(difference)))
                }, fontWeight = FontWeight.Medium)
            }
        }
        if (summary.moodCounts.isNotEmpty() || summary.energyCounts.isNotEmpty() || summary.painDays > 0 || summary.sleepAverage != null) {
            HorizontalDivider()
            Text(stringResource(R.string.month_wellbeing), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.month_observations_only), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            summary.moodCounts.entries.sortedByDescending { it.value }.forEach { (mood, count) ->
                MonthObservation(Icons.Outlined.SentimentSatisfied, stringResource(moodLabel(mood)), count)
            }
            summary.energyCounts.entries.sortedByDescending { it.value }.forEach { (energy, count) ->
                MonthObservation(Icons.Outlined.Bolt, stringResource(R.string.energy_summary, stringResource(wellbeingLevelLabel(energy))), count)
            }
            if (summary.painDays > 0) Text(stringResource(R.string.month_pain_days, summary.painDays))
            summary.sleepAverage?.let { Text(stringResource(R.string.month_sleep_average, String.format(locale, "%.1f", it))) }
        } else {
            Text(stringResource(R.string.month_observations_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonthCount(count: Int, label: String, icon: ImageVector, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { }.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MonthTrack(month: YearMonth, days: Set<LocalDate>, label: String, color: Color) {
    val description = "$label: ${days.sorted().joinToString { it.dayOfMonth.toString() }}"
    val background = MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Canvas(Modifier.fillMaxWidth().height(12.dp).semantics { contentDescription = description }) {
            val cell = size.width / month.lengthOfMonth()
            drawRoundRect(background.copy(alpha = 0.35f), cornerRadius = CornerRadius(size.height / 2))
            days.forEach { day ->
                val left = (day.dayOfMonth - 1) * cell
                drawRoundRect(color, Offset(left, 0f), Size((cell - 1.dp.toPx()).coerceAtLeast(1f), size.height), CornerRadius(3.dp.toPx()))
            }
        }
    }
}

@Composable
private fun MonthDateRow(icon: ImageVector, label: String, value: String, supporting: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
            supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MonthObservation(icon: ImageVector, label: String, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(pluralStringResource(R.plurals.days_value, count, count), style = MaterialTheme.typography.labelLarge)
    }
}

private fun dateRange(start: LocalDate, end: LocalDate, format: DateTimeFormatter): String =
    if (start == end) start.format(format) else "${start.format(format)} – ${end.format(format)}"
