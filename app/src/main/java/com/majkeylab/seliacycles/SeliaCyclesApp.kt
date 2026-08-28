package com.majkeylab.seliacycles

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class Screen(@param:StringRes val label: Int, val icon: ImageVector) {
    TODAY(R.string.nav_today, Icons.Default.Home),
    CALENDAR(R.string.nav_calendar, Icons.Default.DateRange),
    HISTORY(R.string.nav_history, Icons.AutoMirrored.Filled.List),
    SETTINGS(R.string.nav_settings, Icons.Default.Settings),
}

private enum class InfoDialog { PRIVACY, CYCLE }

private enum class SettingsPage(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
    val icon: ImageVector,
) {
    CYCLE(R.string.settings_cycle, R.string.settings_cycle_summary, Icons.Outlined.Tune),
    APPEARANCE(R.string.settings_appearance, R.string.settings_appearance_summary, Icons.Outlined.Palette),
    REMINDERS(R.string.section_reminders, R.string.settings_reminders_summary, Icons.Outlined.Notifications),
    DATA(R.string.settings_data, R.string.settings_data_summary, Icons.Outlined.ImportExport),
    PRIVACY(R.string.section_about, R.string.settings_privacy_summary, Icons.Outlined.Security),
}

@Composable
fun SeliaCyclesApp(
    state: AppState,
    viewModel: MainViewModel,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.TODAY) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var infoDialog by remember { mutableStateOf<InfoDialog?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.saveSettings(state.backup.settings.copy(reminderEnabled = true))
        else viewModel.permissionDenied()
    }

    state.message?.let { message ->
        val text = stringResource(message)
        LaunchedEffect(message) {
            snackbar.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (!state.loading && screen == Screen.CALENDAR) {
                LargeFloatingActionButton(onClick = { selectedDay = LocalDate.now() }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_entry),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = screen == item,
                        onClick = { screen = item },
                        icon = { Icon(item.icon, contentDescription = stringResource(item.label)) },
                        label = { Text(stringResource(item.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                when (screen) {
                    Screen.TODAY -> TodayScreen(state, onEdit = { selectedDay = it })
                    Screen.CALENDAR -> CalendarScreen(state, onEdit = { selectedDay = it })
                    Screen.HISTORY -> HistoryScreen(state)
                    Screen.SETTINGS -> SettingsScreen(
                        state = state,
                        onSave = viewModel::saveSettings,
                        onReminderChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.saveSettings(state.backup.settings.copy(reminderEnabled = enabled))
                            }
                        },
                        onInfo = { infoDialog = it },
                        onDeleteAll = { showDeleteConfirm = true },
                    )
                }
            }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    selectedDay?.let { day ->
        DayLogSheet(
            day = day,
            initial = state.logsByDay[day],
            onDismiss = { selectedDay = null },
            onSave = {
                viewModel.saveLog(it)
                selectedDay = null
            },
        )
    }
    infoDialog?.let { dialog ->
        InfoDialogContent(dialog = dialog, onDismiss = { infoDialog = null })
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_all_data)) },
            text = { Text(stringResource(R.string.delete_all_data_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.clearAll()
                }) { Text(stringResource(R.string.confirm_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun TodayScreen(state: AppState, onEdit: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val prediction = state.prediction
    val predictionsEnabled = state.backup.settings.predictionsEnabled
    val next = prediction.nextPeriodStart.takeIf { predictionsEnabled }
    val distance = next?.let { ChronoUnit.DAYS.between(today, it).toInt() }
    val locale = currentLocale()
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }
    val shortDateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val latestStart = prediction.periodStarts.lastOrNull()
    val cycleDay = latestStart?.let { ChronoUnit.DAYS.between(it, today).toInt() + 1 }?.takeIf { it > 0 }
    val predictedDays = prediction.futurePeriodStarts.takeIf { state.backup.settings.predictionsEnabled }
        ?.flatMap { start -> (0 until prediction.averagePeriodLength).map { start.plusDays(it.toLong()) } }
        ?.toSet()
        .orEmpty()
    val todayLog = state.logsByDay[today]
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.today_heading), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(CycleGradientStart, CycleGradientEnd))).padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.cycle_day), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Text(
                    cycleDay?.let { stringResource(R.string.cycle_day_value, it) } ?: "—",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.24f))
                Text(stringResource(R.string.next_period), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Text(
                    when {
                        !predictionsEnabled -> stringResource(R.string.predictions_disabled)
                        next == null -> stringResource(R.string.no_period_data)
                        prediction.earliestPeriodStart == prediction.latestPeriodStart -> next.format(dateFormat)
                        else -> stringResource(
                            R.string.estimated_window,
                            prediction.earliestPeriodStart?.format(shortDateFormat).orEmpty(),
                            prediction.latestPeriodStart?.format(shortDateFormat).orEmpty(),
                        )
                    },
                    color = Color.White,
                    style = if (next == null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                    fontWeight = if (next == null) FontWeight.Normal else FontWeight.SemiBold,
                )
                when {
                    distance == null -> Unit
                    distance > 0 -> Text(pluralStringResource(R.plurals.days_until_period, distance, distance), color = Color.White)
                    distance == 0 -> Text(stringResource(R.string.predicted_today), color = Color.White)
                    else -> Text(pluralStringResource(R.plurals.period_late, -distance, -distance), color = Color.White)
                }
                if (prediction.periodStarts.isNotEmpty()) {
                    Text(
                        pluralStringResource(
                            R.plurals.based_on_periods,
                            prediction.periodStarts.size,
                            prediction.periodStarts.size,
                        ),
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (predictionsEnabled) {
            MonthlyForecastSection(prediction.monthlyForecasts)
        }
        SectionLabel(Icons.Outlined.CalendarMonth, R.string.week_heading)
        Row(Modifier.fillMaxWidth()) {
            (-3L..3L).forEach { offset ->
                val day = today.plusDays(offset)
                WeekDay(
                    day = day,
                    recorded = state.logsByDay[day]?.bleeding == true,
                    predicted = day in predictedDays,
                    onClick = { onEdit(day) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Button(onClick = { onEdit(today) }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (todayLog == null) R.string.log_today else R.string.edit_today))
        }
        SectionLabel(Icons.Outlined.EventAvailable, R.string.today_summary)
        if (todayLog == null) {
            Text(stringResource(R.string.nothing_logged), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).clickable { onEdit(today) }.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.today_logged), fontWeight = FontWeight.SemiBold)
                }
                if (todayLog.bleeding) {
                    Text(stringResource(R.string.flow_summary, stringResource(flowLabel(todayLog.flow))))
                }
                todayLog.mood?.let { Text(stringResource(R.string.mood_summary, stringResource(moodLabel(it)))) }
                if (todayLog.symptoms.isNotEmpty()) {
                    Text(pluralStringResource(R.plurals.symptom_count, todayLog.symptoms.size, todayLog.symptoms.size))
                }
            }
        }
        if (next != null) Text(
            stringResource(R.string.estimate_notice),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MonthlyForecastSection(forecasts: List<MonthlyForecast>) {
    val locale = currentLocale()
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(Icons.Outlined.EventAvailable, R.string.forecast_heading)
        forecasts.forEachIndexed { index, forecast ->
            val icon = when (forecast.status) {
                ForecastStatus.RECORDED -> Icons.Outlined.CheckCircle
                ForecastStatus.ESTIMATED -> Icons.Outlined.EventAvailable
                ForecastStatus.NOT_EXPECTED -> Icons.Outlined.CalendarMonth
                ForecastStatus.UNAVAILABLE -> Icons.Outlined.CalendarMonth
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        stringResource(if (index == 0) R.string.this_month else R.string.following_month),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        when (forecast.status) {
                            ForecastStatus.RECORDED -> stringResource(
                                R.string.forecast_recorded,
                                forecast.start?.format(dateFormat).orEmpty(),
                            )
                            ForecastStatus.ESTIMATED -> stringResource(
                                R.string.forecast_estimated,
                                forecast.earliestStart?.format(dateFormat).orEmpty(),
                                forecast.latestStart?.format(dateFormat).orEmpty(),
                            )
                            ForecastStatus.NOT_EXPECTED -> stringResource(R.string.forecast_not_expected)
                            ForecastStatus.UNAVAILABLE -> stringResource(R.string.forecast_unavailable)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDay(
    day: LocalDate,
    recorded: Boolean,
    predicted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val background = when {
        recorded -> MaterialTheme.colorScheme.primary
        predicted -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val foreground = when {
        recorded -> MaterialTheme.colorScheme.onPrimary
        predicted -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(background)
            .then(if (day == LocalDate.now()) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)) else Modifier)
            .clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 2.dp)
            .semantics { contentDescription = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale), color = foreground, style = MaterialTheme.typography.labelSmall)
        Text(day.dayOfMonth.toString(), color = foreground, fontWeight = FontWeight.SemiBold)
    }
}

@StringRes
private fun flowLabel(flow: Flow): Int = when (flow) {
    Flow.NONE -> R.string.flow_none
    Flow.UNKNOWN -> R.string.flow_unknown
    Flow.LIGHT -> R.string.flow_light
    Flow.MEDIUM -> R.string.flow_medium
    Flow.HEAVY -> R.string.flow_heavy
}

@StringRes
private fun moodLabel(mood: Mood): Int = when (mood) {
    Mood.GREAT -> R.string.mood_great
    Mood.GOOD -> R.string.mood_good
    Mood.OKAY -> R.string.mood_okay
    Mood.LOW -> R.string.mood_low
    Mood.BAD -> R.string.mood_bad
}

@Composable
private fun CalendarScreen(
    state: AppState,
    onEdit: (LocalDate) -> Unit,
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val shownMonth = YearMonth.parse(month)
    val locale = currentLocale()
    val firstDay = state.backup.settings.firstDayOfWeek
    val weekdays = if (firstDay == DayOfWeek.MONDAY) DayOfWeek.entries else listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)
    val leading = (shownMonth.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val cells = leading + shownMonth.lengthOfMonth()
    val rows = (cells + 6) / 7
    val predicted = state.prediction.futurePeriodStarts.takeIf { state.backup.settings.predictionsEnabled }
        ?.flatMap { start ->
            (0 until state.prediction.averagePeriodLength).map { start.plusDays(it.toLong()) }
        }
        ?.toSet()
        .orEmpty()
    val previousMonthLabel = stringResource(R.string.previous_month)
    val nextMonthLabel = stringResource(R.string.next_month)
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
        Text(stringResource(R.string.calendar_heading), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { month = shownMonth.minusMonths(1).toString() },
                enabled = shownMonth > YearMonth.from(DayLog.MIN_DATE),
            ) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = previousMonthLabel)
            }
            Text(
                shownMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(
                onClick = { month = shownMonth.plusMonths(1).toString() },
                enabled = shownMonth < YearMonth.from(DayLog.MAX_DATE),
            ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = nextMonthLabel)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { weekday ->
                Text(
                    weekday.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - leading + 1
                    if (dayNumber !in 1..shownMonth.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = shownMonth.atDay(dayNumber)
                        CalendarDay(
                            day = day,
                            recorded = state.logsByDay[day]?.bleeding == true,
                            predicted = day in predicted,
                            onClick = { onEdit(day) },
                            enabled = true,
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                        )
                    }
                }
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LegendDot(MaterialTheme.colorScheme.primary, stringResource(R.string.recorded_legend))
            LegendDot(MaterialTheme.colorScheme.secondaryContainer, stringResource(R.string.predicted_legend))
        }
    }
}

@Composable
private fun CalendarDay(
    day: LocalDate,
    recorded: Boolean,
    predicted: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    val background = when {
        recorded -> MaterialTheme.colorScheme.primary
        predicted -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val foreground = when {
        recorded -> MaterialTheme.colorScheme.onPrimary
        predicted -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val date = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(currentLocale()))
    Box(
        modifier = modifier.padding(3.dp).clip(CircleShape).background(background)
            .then(if (day == LocalDate.now()) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick).semantics { contentDescription = date },
        contentAlignment = Alignment.Center,
    ) { Text(day.dayOfMonth.toString(), color = foreground) }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun HistoryScreen(state: AppState) {
    val locale = currentLocale()
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val prediction = state.prediction
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.history_heading), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(Icons.Outlined.Refresh, R.string.average_cycle, pluralStringResource(R.plurals.days_value, prediction.averageCycleLength, prediction.averageCycleLength), Modifier.weight(1f))
            Metric(Icons.Outlined.Opacity, R.string.average_period, pluralStringResource(R.plurals.days_value, prediction.averagePeriodLength, prediction.averagePeriodLength), Modifier.weight(1f))
            Metric(Icons.Outlined.EventAvailable, R.string.recorded_cycles, prediction.periodStarts.size.toString(), Modifier.weight(1f))
        }
        HorizontalDivider()
        if (prediction.periodStarts.isEmpty()) {
            Text(stringResource(R.string.no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            prediction.periodStarts.asReversed().forEach { start ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(start.format(dateFormat), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.period_started), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(icon: ImageVector, @StringRes label: Int, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SettingsScreen(
    state: AppState,
    onSave: (AppSettings) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onInfo: (InfoDialog) -> Unit,
    onDeleteAll: () -> Unit,
) {
    val settings = state.backup.settings
    var pageName by rememberSaveable { mutableStateOf<String?>(null) }
    val page = pageName?.let(SettingsPage::valueOf)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (page == null) {
            Text(stringResource(R.string.settings_heading), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.settings_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsPage.entries.forEach { item ->
                SettingsCategoryRow(item.icon, item.title, item.summary) { pageName = item.name }
            }
        } else {
            TextButton(onClick = { pageName = null }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_back))
            }
            Text(stringResource(page.title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            when (page) {
                SettingsPage.CYCLE -> {
                    SwitchRow(R.string.predictions, settings.predictionsEnabled, Icons.Outlined.Refresh) {
                        onSave(settings.copy(predictionsEnabled = it))
                    }
                    Stepper(R.string.default_cycle_length, settings.cycleLength, 15..90, Icons.Outlined.Refresh) {
                        onSave(settings.copy(cycleLength = it))
                    }
                    Stepper(R.string.default_period_length, settings.periodLength, 1..14, Icons.Outlined.Opacity) {
                        onSave(settings.copy(periodLength = it))
                    }
                    ChoiceRow(
                        label = R.string.first_day_of_week,
                        choices = listOf(DayOfWeek.MONDAY to R.string.monday, DayOfWeek.SUNDAY to R.string.sunday),
                        selected = settings.firstDayOfWeek,
                        icon = Icons.Outlined.CalendarMonth,
                    ) { onSave(settings.copy(firstDayOfWeek = it)) }
                    InfoBlock(R.string.daily_measurements, R.string.daily_measurements_body, Icons.Outlined.MonitorWeight)
                }
                SettingsPage.APPEARANCE -> {
                    ChoiceRow(
                        label = R.string.theme,
                        choices = listOf(
                            AppTheme.SYSTEM to R.string.theme_system,
                            AppTheme.LIGHT to R.string.theme_light,
                            AppTheme.DARK to R.string.theme_dark,
                        ),
                        selected = settings.theme,
                        icon = Icons.Outlined.Palette,
                    ) { onSave(settings.copy(theme = it)) }
                    LanguageRow()
                }
                SettingsPage.REMINDERS -> {
                    SwitchRow(R.string.period_reminder, settings.reminderEnabled, Icons.Outlined.Notifications, onReminderChange)
                    if (settings.reminderEnabled) {
                        Stepper(R.string.remind_before, settings.reminderDays, 0..14, Icons.Outlined.Notifications) {
                            onSave(settings.copy(reminderDays = it))
                        }
                    }
                }
                SettingsPage.DATA -> {
                    InfoBlock(R.string.device_transfer, R.string.device_transfer_body, Icons.Outlined.Security)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    OutlinedButton(onClick = onDeleteAll, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_all_data), color = MaterialTheme.colorScheme.error)
                    }
                }
                SettingsPage.PRIVACY -> {
                    SettingsLink(Icons.Outlined.Security, R.string.privacy) { onInfo(InfoDialog.PRIVACY) }
                    SettingsLink(Icons.Outlined.HealthAndSafety, R.string.about_cycle) { onInfo(InfoDialog.CYCLE) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsCategoryRow(icon: ImageVector, @StringRes title: Int, @StringRes summary: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(summary), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, @StringRes text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(text), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SwitchRow(@StringRes label: Int, checked: Boolean, icon: ImageVector? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        icon?.let {
            Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
        }
        Text(stringResource(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Stepper(
    @StringRes label: Int,
    value: Int,
    range: IntRange,
    icon: ImageVector? = null,
    onChange: (Int) -> Unit,
) {
    val labelText = stringResource(label)
    val decrease = stringResource(R.string.decrease_value, labelText)
    val increase = stringResource(R.string.increase_value, labelText)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        icon?.let {
            Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
        }
        Text(labelText, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) {
            Icon(Icons.Outlined.Remove, contentDescription = decrease)
        }
        Text(value.toString(), modifier = Modifier.width(36.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) {
            Icon(Icons.Default.Add, contentDescription = increase)
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    @StringRes label: Int,
    choices: List<Pair<T, Int>>,
    selected: T?,
    icon: ImageVector? = null,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon == null) Text(stringResource(label)) else SectionLabel(icon, label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            choices.forEach { (value, text) ->
                FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(stringResource(text)) })
            }
        }
    }
}

@Composable
private fun LanguageRow() {
    val current = AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()
    ChoiceRow(
        label = R.string.language,
        choices = listOf(
            "" to R.string.language_system,
            "en" to R.string.language_english,
            "cs" to R.string.language_czech,
            "sk" to R.string.language_slovak,
            "de" to R.string.language_german,
            "pl" to R.string.language_polish,
            "es" to R.string.language_spanish,
        ),
        selected = current,
        icon = Icons.Outlined.Language,
    ) { language -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language)) }
}

@Composable
private fun InfoBlock(@StringRes title: Int, @StringRes body: Int, icon: ImageVector? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        icon?.let { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsLink(icon: ImageVector, @StringRes label: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DayLogSheet(day: LocalDate, initial: DayLog?, onDismiss: () -> Unit, onSave: (DayLog) -> Unit) {
    var flow by remember(day, initial) { mutableStateOf(initial?.flow?.takeIf { initial.bleeding } ?: Flow.NONE) }
    var mood by remember(day, initial) { mutableStateOf(initial?.mood) }
    var symptoms by remember(day, initial) { mutableStateOf(initial?.symptoms.orEmpty()) }
    var note by remember(day, initial) { mutableStateOf(initial?.note.orEmpty()) }
    var showMore by rememberSaveable(day) { mutableStateOf(false) }
    var weight by remember(day, initial) { mutableStateOf(initial?.weightKg?.toString().orEmpty()) }
    var temperature by remember(day, initial) { mutableStateOf(initial?.temperatureC?.toString().orEmpty()) }
    var sleep by remember(day, initial) { mutableStateOf(initial?.sleepHours?.toString().orEmpty()) }
    var intimacy by remember(day, initial) { mutableStateOf(initial?.intimacy) }
    val weightValue = parseDecimal(weight)
    val temperatureValue = parseDecimal(temperature)
    val sleepValue = parseDecimal(sleep)
    val weightValid = weight.isBlank() || weightValue != null && weightValue in DayLog.MIN_WEIGHT_KG..DayLog.MAX_WEIGHT_KG
    val temperatureValid = temperature.isBlank() || temperatureValue != null &&
        temperatureValue in DayLog.MIN_TEMPERATURE_C..DayLog.MAX_TEMPERATURE_C
    val sleepValid = sleep.isBlank() || sleepValue != null && sleepValue in 0.0..24.0
    val canSave = weightValid && temperatureValid && sleepValid
    val locale = currentLocale()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding(),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(stringResource(R.string.edit_day), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChoiceRow(
                    label = R.string.flow,
                    choices = listOf(
                        Flow.NONE to R.string.flow_none,
                        Flow.UNKNOWN to R.string.flow_unknown,
                        Flow.LIGHT to R.string.flow_light,
                        Flow.MEDIUM to R.string.flow_medium,
                        Flow.HEAVY to R.string.flow_heavy,
                    ),
                    selected = flow,
                    icon = Icons.Outlined.Opacity,
                ) { flow = it }
                ChoiceRow(
                    label = R.string.mood,
                    choices = listOf(
                        Mood.GREAT to R.string.mood_great,
                        Mood.GOOD to R.string.mood_good,
                        Mood.OKAY to R.string.mood_okay,
                        Mood.LOW to R.string.mood_low,
                        Mood.BAD to R.string.mood_bad,
                    ),
                    selected = mood,
                    icon = Icons.Outlined.SentimentSatisfied,
                ) { mood = if (mood == it) null else it }
                SectionLabel(Icons.Outlined.Healing, R.string.symptoms)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    symptomLabels.forEach { (symptom, label) ->
                        FilterChip(
                            selected = symptom in symptoms,
                            onClick = { symptoms = if (symptom in symptoms) symptoms - symptom else symptoms + symptom },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= DayLog.MAX_NOTE_LENGTH) note = it },
                    label = { Text(stringResource(R.string.note)) },
                    placeholder = { Text(stringResource(R.string.note_hint)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                TextButton(onClick = { showMore = !showMore }) {
                    Icon(Icons.Outlined.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (showMore) R.string.fewer_details else R.string.more_details))
                }
                if (showMore) {
                    MeasurementField(weight, { weight = it }, R.string.weight_kg, weightValid, Icons.Outlined.MonitorWeight)
                    MeasurementField(temperature, { temperature = it }, R.string.temperature_c, temperatureValid, Icons.Outlined.Thermostat)
                    MeasurementField(sleep, { sleep = it }, R.string.sleep_hours, sleepValid, Icons.Outlined.Bedtime)
                    SectionLabel(Icons.Outlined.FavoriteBorder, R.string.intimacy)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Intimacy.SEX to R.string.intimacy_sex,
                            Intimacy.PROTECTED to R.string.intimacy_protected,
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = intimacy == value,
                                onClick = { intimacy = value.takeUnless { intimacy == value } },
                                label = { Text(stringResource(label)) },
                            )
                        }
                    }
                    initial?.importedDetails?.takeIf(String::isNotBlank)?.let { imported ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SectionLabel(Icons.Outlined.ImportExport, R.string.imported_details)
                            Text(imported, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (initial != null) TextButton(onClick = { onSave(DayLog(day)) }) { Text(stringResource(R.string.delete_record)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = {
                    onSave(DayLog(
                        day = day,
                        bleeding = flow != Flow.NONE,
                        flow = flow,
                        mood = mood,
                        symptoms = symptoms,
                        note = note.trim(),
                        weightKg = weightValue,
                        temperatureC = temperatureValue,
                        sleepHours = sleepValue,
                        intimacy = intimacy,
                        importedDetails = initial?.importedDetails.orEmpty(),
                    ))
                }, enabled = canSave) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@Composable
private fun MeasurementField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    valid: Boolean,
    icon: ImageVector,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.length <= 8) onValueChange(input.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(stringResource(label)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        supportingText = if (valid) null else {{ Text(stringResource(R.string.invalid_measurement)) }},
        isError = !valid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun parseDecimal(value: String): Double? = value.trim().replace(',', '.').takeIf(String::isNotEmpty)?.toDoubleOrNull()

private val symptomLabels = listOf(
    Symptom.CRAMPS to R.string.symptom_cramps,
    Symptom.HEADACHE to R.string.symptom_headache,
    Symptom.BLOATING to R.string.symptom_bloating,
    Symptom.TENDER_BREASTS to R.string.symptom_tender_breasts,
    Symptom.FATIGUE to R.string.symptom_fatigue,
    Symptom.ACNE to R.string.symptom_acne,
    Symptom.CRAVINGS to R.string.symptom_cravings,
    Symptom.BACKACHE to R.string.symptom_backache,
)

@Composable
private fun InfoDialogContent(dialog: InfoDialog, onDismiss: () -> Unit) {
    val title = if (dialog == InfoDialog.PRIVACY) R.string.privacy else R.string.about_cycle
    val body = if (dialog == InfoDialog.PRIVACY) R.string.privacy_body else R.string.cycle_info_body
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(body))
                if (dialog == InfoDialog.CYCLE) {
                    Text(stringResource(R.string.medical_sources), fontWeight = FontWeight.SemiBold)
                    FlowRow {
                        SourceLink(R.string.source_who, "https://www.who.int/news-room/fact-sheets/detail/menstrual-health")
                        SourceLink(R.string.source_owh, "https://womenshealth.gov/menstrual-cycle/your-menstrual-cycle")
                        SourceLink(R.string.source_nhs, "https://www.nhs.uk/conditions/periods/fertility-in-the-menstrual-cycle/")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
private fun SourceLink(@StringRes label: Int, url: String) {
    val context = LocalContext.current
    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }) {
        Text(stringResource(label))
    }
}
