package com.majkeylab.seliacycles

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

internal fun calendarWidgetViews(
    context: Context, id: Int, profile: LocalProfile, content: CycleContent, month: YearMonth, details: Boolean,
    appearance: WidgetAppearance = WidgetAppearance(),
): RemoteViews {
    val settings = content.backup.settings
    val dark = settings.theme == AppTheme.DARK || settings.theme == AppTheme.SYSTEM &&
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val lightColors = appearance.colors(paletteColorScheme(settings.palette, settings.customPalette, false))
    val darkColors = appearance.colors(paletteColorScheme(settings.palette, settings.customPalette, true))
    val colors = if (dark) darkColors else lightColors
    fun RemoteViews.color(view: Int, method: String, select: (ColorScheme) -> Color) {
        if (settings.theme == AppTheme.SYSTEM && Build.VERSION.SDK_INT >= 31) {
            setColorInt(view, method, select(lightColors).toArgb(), select(darkColors).toArgb())
        } else setInt(view, method, select(colors).toArgb())
    }
    val locale = context.resources.configuration.locales[0]
    val days = CalendarPaging.gridDays(month, settings.firstDayOfWeek)
    val tracks = CalendarTracks(content, profile.mode != UiMode.SIMPLE && settings.canEstimateFertility)
    val period = calendarPeriodRgb(settings.palette, settings.customPalette).color()
    val entry = calendarEntryRgb(settings.palette, settings.customPalette).color()
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    return RemoteViews(context.packageName, R.layout.calendar_widget).apply {
        color(R.id.widget_background, "setColorFilter") { it.surface }
        setInt(R.id.widget_background, "setImageAlpha", appearance.backgroundAlpha)
        setTextViewText(R.id.widget_brand, context.getString(R.string.app_name) + " · " + profile.name.ifBlank { context.getString(R.string.profile_default_name) })
        color(R.id.widget_brand, "setTextColor") { it.onSurfaceVariant }
        setTextViewText(R.id.widget_today, context.getString(R.string.today_heading))
        setContentDescription(R.id.widget_previous, context.getString(R.string.previous_month))
        setContentDescription(R.id.widget_next, context.getString(R.string.next_month))
        setContentDescription(R.id.widget_settings, context.getString(R.string.widget_configure))
        setTextViewText(R.id.widget_month, month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) })
        listOf(R.id.widget_month, R.id.widget_previous, R.id.widget_next, R.id.widget_settings, R.id.widget_today).forEach {
            color(it, "setTextColor") { scheme -> scheme.primary }
        }
        setOnClickPendingIntent(R.id.widget_month, CalendarWidget.openDay(context, id, profile.id, month.atDay(1)))
        setOnClickPendingIntent(R.id.widget_settings, CalendarWidget.configureClick(context, id))
        setOnClickPendingIntent(R.id.widget_previous, CalendarWidget.monthClick(context, id, -1))
        setOnClickPendingIntent(R.id.widget_next, CalendarWidget.monthClick(context, id, 1))
        setOnClickPendingIntent(R.id.widget_today, CalendarWidget.monthClick(context, id, 0))
        setBoolean(R.id.widget_previous, "setEnabled", month > YearMonth.from(DayLog.MIN_DATE))
        setBoolean(R.id.widget_next, "setEnabled", month < YearMonth.from(DayLog.MAX_DATE))
        setViewVisibility(R.id.widget_message, View.GONE)
        setViewVisibility(R.id.widget_navigation, View.VISIBLE)
        setViewVisibility(R.id.widget_today, View.VISIBLE)
        removeAllViews(R.id.widget_weekdays)
        days.take(7).forEach { day -> addView(R.id.widget_weekdays, RemoteViews(context.packageName, R.layout.calendar_widget_weekday).apply {
            setTextViewText(R.id.widget_weekday, day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale))
            color(R.id.widget_weekday, "setTextColor") { it.onSurfaceVariant }
        }) }
        removeAllViews(R.id.widget_weeks)
        days.chunked(7).forEach { week ->
            val row = RemoteViews(context.packageName, R.layout.calendar_widget_week)
            week.forEachIndexed { column, day ->
                val state = if (details) tracks.forDay(day) else CalendarDayTracks(CalendarPeriodLayer.NONE, false, false, false)
                val inMonth = YearMonth.from(day) == month
                val opacity = if (inMonth) 1f else 0.42f
                val recorded = state.period == CalendarPeriodLayer.RECORDED
                val periodFill = when (state.period) {
                    CalendarPeriodLayer.RECORDED -> period
                    CalendarPeriodLayer.PREDICTED -> calendarPredictedPeriodColor(period)
                    CalendarPeriodLayer.NONE -> Color.Transparent
                }.let { it.copy(alpha = it.alpha * opacity) }
                val fertilityFill = if (state.fertile) colors.tertiary.copy(alpha = 0.20f * opacity) else Color.Transparent
                fun foreground(scheme: ColorScheme): Color {
                    val fertile = if (state.fertile) scheme.tertiary.copy(alpha = 0.20f * opacity) else Color.Transparent
                    val ovulation = if (state.ovulation) scheme.primary.copy(alpha = 0.20f * opacity) else Color.Transparent
                    val background = ovulation.compositeOver(periodFill).compositeOver(fertile).compositeOver(scheme.surface)
                    val text = if (!inMonth) scheme.onSurfaceVariant else if (recorded) period.contrastColor() else scheme.onSurface
                    // Keep explicit text colors on wallpaper; opaque period bands still need readable dates.
                    return if (appearance.textRgb != null && !recorded) text else text.readableOn(background)
                }
                val hasNote = details && content.logsByDay[day]?.hasCalendarMarker == true
                row.addView(R.id.widget_week, RemoteViews(context.packageName, R.layout.calendar_widget_day).apply {
                    setTextViewText(R.id.widget_date, day.dayOfMonth.toString())
                    color(R.id.widget_date, "setTextColor", ::foreground)
                    fun band(view: Int, fill: Color, connects: (LocalDate) -> Boolean) {
                        val left = column > 0 && connects(day.minusDays(1))
                        val right = column < 6 && connects(day.plusDays(1))
                        setImageViewResource(view, when {
                            left && right -> R.drawable.widget_band_middle
                            left -> R.drawable.widget_band_end
                            right -> R.drawable.widget_band_start
                            else -> R.drawable.widget_band_single
                        })
                        tint(view, fill)
                        setInt(view, "setImageAlpha", (fill.alpha * 255).toInt())
                    }
                    band(R.id.widget_period, periodFill) { tracks.forDay(it).period == state.period }
                    band(R.id.widget_fertile, fertilityFill) { tracks.forDay(it).fertile }
                    color(R.id.widget_fertile, "setColorFilter") { it.tertiary }
                    color(R.id.widget_ovulation, "setColorFilter") { it.primary }
                    setInt(R.id.widget_ovulation, "setImageAlpha", if (state.ovulation) (51 * opacity).toInt() else 0)
                    color(R.id.widget_today_ring, "setColorFilter") { it.primary }
                    setViewVisibility(R.id.widget_today_ring, if (day == content.referenceDate) View.VISIBLE else View.GONE)
                    color(R.id.widget_note, "setColorFilter") { entry.readableOn(it.surface) }
                    setViewVisibility(R.id.widget_note, if (hasNote) View.VISIBLE else View.GONE)
                    setContentDescription(R.id.widget_day, buildList {
                        add(day.format(dateFormat))
                        if (day == content.referenceDate) add(context.getString(R.string.today_heading))
                        if (recorded) add(context.getString(R.string.recorded_legend))
                        if (state.period == CalendarPeriodLayer.PREDICTED || state.predictedOverlap) add(context.getString(R.string.predicted_legend))
                        if (state.fertile) add(context.getString(R.string.fertile_legend))
                        if (state.ovulation) add(context.getString(R.string.ovulation_legend))
                        if (hasNote) add(context.getString(R.string.recorded_values))
                    }.joinToString(", "))
                    if (day in DayLog.MIN_DATE..DayLog.MAX_DATE) setOnClickPendingIntent(R.id.widget_day, CalendarWidget.openDay(context, id, profile.id, day))
                })
            }
            addView(R.id.widget_weeks, row)
        }
    }
}

private fun RemoteViews.tint(view: Int, color: Color) = setInt(view, "setColorFilter", color.copy(alpha = 1f).toArgb())
