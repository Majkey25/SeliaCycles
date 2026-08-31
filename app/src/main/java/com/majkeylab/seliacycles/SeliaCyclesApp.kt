package com.majkeylab.seliacycles

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AirlineSeatReclineNormal
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChangeCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChildFriendly
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PregnantWoman
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.PsychologyAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
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
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private const val PRIVACY_POLICY_URL = "https://majkey25.github.io/SeliaCycles/"
private const val SUPPORT_URL = "https://www.buymeacoffee.com/majkey"

private enum class Screen(
    @param:StringRes val label: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    TODAY(R.string.nav_today, Icons.Filled.Home, Icons.Outlined.Home),
    CALENDAR(R.string.nav_calendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    HISTORY(R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
}

private enum class InfoDialog { PRIVACY, CYCLE }

private enum class DaySheetMode { OVERVIEW, EDIT }

private data class ChoiceOption<T>(
    val value: T,
    @param:StringRes val label: Int,
    val icon: ImageVector? = null,
)

private enum class CustomColorTarget(@param:StringRes val label: Int) {
    PRIMARY(R.string.custom_primary_color),
    SECONDARY(R.string.custom_secondary_color),
    TERTIARY(R.string.custom_tertiary_color),
    ENTRY(R.string.custom_entry_color),
}

private enum class SelfCareActivity(
    @param:StringRes val title: Int,
    @param:StringRes val instructions: Int,
    val minutes: Int,
    val icon: ImageVector,
) {
    BREATHING(R.string.self_care_breathing, R.string.self_care_breathing_steps, 3, Icons.Outlined.Air),
    MOVEMENT(R.string.self_care_movement, R.string.self_care_movement_steps, 5, Icons.Outlined.AccessibilityNew),
    WALK(R.string.self_care_walk, R.string.self_care_walk_steps, 10, Icons.AutoMirrored.Outlined.DirectionsWalk),
    HEAT(R.string.self_care_heat, R.string.self_care_heat_steps, 15, Icons.Outlined.Thermostat),
    MASSAGE(R.string.self_care_massage, R.string.self_care_massage_steps, 3, Icons.Outlined.Healing),
    HYDRATION(R.string.self_care_hydration, R.string.self_care_hydration_steps, 2, Icons.Outlined.WaterDrop),
    REST(R.string.self_care_rest, R.string.self_care_rest_steps, 10, Icons.Outlined.Bedtime),
    FOOT_MASSAGE(
        R.string.self_care_foot_massage,
        R.string.self_care_foot_massage_steps,
        4,
        Icons.Outlined.AirlineSeatReclineNormal,
    ),
}

private enum class SettingsPage(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
    val icon: ImageVector,
) {
    CYCLE(R.string.settings_cycle, R.string.settings_cycle_summary, Icons.Outlined.Autorenew),
    PROFILE(R.string.settings_profile, R.string.settings_profile_summary, Icons.Outlined.PersonOutline),
    HOME(R.string.settings_home, R.string.settings_home_summary, Icons.Outlined.Home),
    APPEARANCE(R.string.settings_appearance, R.string.settings_appearance_summary, Icons.Outlined.Palette),
    REMINDERS(R.string.section_reminders, R.string.settings_reminders_summary, Icons.Outlined.NotificationsNone),
    CALENDAR(R.string.settings_calendar, R.string.settings_calendar_summary, Icons.Outlined.EventRepeat),
    DATA(R.string.settings_data, R.string.settings_data_summary, Icons.Outlined.Devices),
    PRIVACY(R.string.section_about, R.string.settings_privacy_summary, Icons.Outlined.VerifiedUser),
}

@Composable
fun SeliaCyclesApp(
    state: AppState,
    viewModel: MainViewModel,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.TODAY) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var daySheetMode by remember { mutableStateOf(DaySheetMode.OVERVIEW) }
    var infoDialog by remember { mutableStateOf<InfoDialog?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSelfCare by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val openMyCalendar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.inspectMyCalendar(uri)
    }
    val createMyCalendar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> if (uri != null) viewModel.exportMyCalendar(uri) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.saveSettings(state.backup.settings.copy(reminderEnabled = true))
        else viewModel.permissionDenied()
    }
    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (CalendarMirror.REQUIRED_PERMISSIONS.all { result[it] == true }) viewModel.calendarPermissionChanged()
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
        bottomBar = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NavigationBar(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Screen.entries.forEach { item ->
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screen = item },
                            icon = {
                                Icon(
                                    if (screen == item) item.selectedIcon else item.icon,
                                    contentDescription = stringResource(item.label),
                                )
                            },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier.fillMaxHeight().widthIn(max = 600.dp).fillMaxWidth()
                    .align(Alignment.TopCenter),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    when (screen) {
                        Screen.TODAY -> TodayScreen(
                            state,
                            onOpenDay = {
                                selectedDay = it
                                daySheetMode = DaySheetMode.OVERVIEW
                            },
                            onStartPeriod = { viewModel.startPeriod(LocalDate.now()) },
                            onEndPeriod = {
                                viewModel.endPeriod(LocalDate.now(), suggestedPeriodStart(state, LocalDate.now()))
                            },
                            onSelfCare = { showSelfCare = true },
                        )
                        Screen.CALENDAR -> CalendarScreen(state, onEdit = {
                            selectedDay = it
                            daySheetMode = DaySheetMode.OVERVIEW
                        })
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
                            onMyCalendarImport = { openMyCalendar.launch(arrayOf("application/octet-stream", "*/*")) },
                            onMyCalendarExport = {
                                createMyCalendar.launch("Selia-Cycles-${LocalDate.now()}.pc")
                            },
                            onRequestCalendarPermission = {
                                calendarPermission.launch(CalendarMirror.REQUIRED_PERMISSIONS)
                            },
                            onCalendarSelect = viewModel::connectCalendar,
                            onCalendarDisconnect = viewModel::disconnectCalendar,
                        )
                    }
                }
            }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    selectedDay?.let { day ->
        if (daySheetMode == DaySheetMode.OVERVIEW) {
            DayOverviewSheet(
                day = day,
                state = state,
                onDismiss = { selectedDay = null },
                onEdit = { daySheetMode = DaySheetMode.EDIT },
                onStartPeriod = {
                    viewModel.startPeriod(day)
                    selectedDay = null
                },
                onEndPeriod = {
                    viewModel.endPeriod(day, suggestedPeriodStart(state, day))
                    selectedDay = null
                },
                onRemovePeriod = {
                    viewModel.removePeriod(day)
                    selectedDay = null
                },
                onSelfCare = { showSelfCare = true },
            )
        } else {
            DayLogSheet(
                day = day,
                initial = state.logsByDay[day],
                showFertility = state.backup.settings.canEstimateFertility,
                onDismiss = { daySheetMode = DaySheetMode.OVERVIEW },
                onSave = {
                    viewModel.saveLog(it)
                    selectedDay = null
                    daySheetMode = DaySheetMode.OVERVIEW
                },
            )
        }
    }
    infoDialog?.let { dialog ->
        InfoDialogContent(dialog = dialog, onDismiss = { infoDialog = null })
    }
    state.myCalendarPreview?.let { preview ->
        MyCalendarPreviewDialog(
            preview = preview,
            onDismiss = viewModel::cancelMyCalendarImport,
            onConfirm = viewModel::confirmMyCalendarImport,
        )
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
    if (showSelfCare) SelfCareSheet(onDismiss = { showSelfCare = false })
}

@Composable
private fun TodayScreen(
    state: AppState,
    onOpenDay: (LocalDate) -> Unit,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit,
    onSelfCare: () -> Unit,
) {
    val today = LocalDate.now()
    val prediction = state.prediction
    val predictionsEnabled = state.backup.settings.canPredictPeriods
    val insight = state.todayInsight
    val next = insight.nextPeriodStart.takeIf { predictionsEnabled }
    val distance = next?.let { ChronoUnit.DAYS.between(today, it).toInt() }
    val locale = currentLocale()
    val shortDateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val latestStart = prediction.periodStarts.lastOrNull { !it.isAfter(today) }
    val cycleDay = latestStart?.let { ChronoUnit.DAYS.between(it, today).toInt() + 1 }?.takeIf { it > 0 }
    val predictedDays = state.periodEstimates.flatMap { estimate ->
        generateSequence(estimate.start) { it.plusDays(1) }.takeWhile { it < estimate.endExclusive }.toList()
    }.toSet()
    val recordedDays = state.backup.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day)
    val fertileDays = insight.fertility?.let { fertility ->
        generateSequence(fertility.fertileStart) { it.plusDays(1) }
            .takeWhile { !it.isAfter(fertility.fertileEnd) }.toSet()
    }.orEmpty()
    val ovulationDays = insight.fertility?.let { setOf(it.ovulation) }.orEmpty()
    val weekTracks: (LocalDate) -> CalendarDayTracks = { day ->
        calendarDayTracks(day, recordedDays, predictedDays, fertileDays, ovulationDays)
    }
    val periodColor = calendarPeriodRgb(state.backup.settings.palette, state.backup.settings.customPalette).color()
    val onPeriodColor = periodColor.contrastColor()
    val todayAction = PeriodActions.todayAction(state.backup.settings, today)
    val primaryOnClick: () -> Unit = when (todayAction) {
        TodayPrimaryAction.START_PERIOD -> onStartPeriod
        TodayPrimaryAction.END_PERIOD -> onEndPeriod
        TodayPrimaryAction.OPEN_LOG -> fun() { onOpenDay(today) }
    }
    var showCycleDetails by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.today_heading), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(paletteGradientColors(
                    state.backup.settings.palette,
                    state.backup.settings.customPalette,
                )))
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(if (predictionsEnabled) R.string.cycle_day else R.string.life_situation),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            if (predictionsEnabled) {
                                cycleDay?.let { stringResource(R.string.cycle_day_value, it) }
                                    ?: stringResource(R.string.no_cycle_yet)
                            } else {
                                stringResource(lifeSituationLabel(state.backup.settings.profile.lifeSituation))
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    distance?.takeIf { predictionsEnabled && it >= 0 }?.let { days ->
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.next_period),
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                pluralStringResource(R.plurals.days_until_period, days, days),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                insight.phase?.let {
                    Text(
                        stringResource(phaseHeadingLabel(it)),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        Button(
            onClick = primaryOnClick,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().height(60.dp),
        ) {
            Icon(
                when (todayAction) {
                    TodayPrimaryAction.START_PERIOD -> Icons.Outlined.Opacity
                    TodayPrimaryAction.END_PERIOD -> Icons.Outlined.CheckCircle
                    TodayPrimaryAction.OPEN_LOG -> Icons.Outlined.Tune
                },
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(when (todayAction) {
                TodayPrimaryAction.START_PERIOD -> R.string.period_start_action
                TodayPrimaryAction.END_PERIOD -> R.string.period_end_action
                TodayPrimaryAction.OPEN_LOG -> R.string.add_entry
            }))
        }
        UpcomingCycleSection(
            insight = insight,
            predictionsEnabled = predictionsEnabled,
            dateFormat = shortDateFormat,
            distance = distance,
        )
        if (prediction.periodStarts.size in 1..2) {
            InfoBlock(R.string.first_cycles_title, R.string.first_cycles_body, Icons.Outlined.Autorenew)
        }
        if (state.backup.settings.showPhaseGuidance) {
            PhaseGuidanceCard(insight, onSelfCare.takeIf { state.backup.settings.showSelfCare })
        } else if (state.backup.settings.showSelfCare && insight.phase == CyclePhase.MENSTRUAL) {
            OutlinedButton(onClick = onSelfCare, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Healing, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.self_care_title))
            }
        }
        if (state.backup.settings.showCycleDetails) {
            TextButton(
                onClick = { showCycleDetails = !showCycleDetails },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Outlined.Insights, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.more_about_cycle))
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (showCycleDetails) Icons.Outlined.Remove else Icons.Default.Add,
                    contentDescription = null,
                )
            }
        }
        if (state.backup.settings.showCycleDetails && showCycleDetails) {
            insight.moodTrend?.let { trend ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.SentimentSatisfied, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        pluralStringResource(
                            R.plurals.mood_trend_value,
                            trend.sampleCount,
                            stringResource(moodLabel(trend.mood)),
                            trend.sampleCount,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SectionLabel(Icons.Outlined.CalendarMonth, R.string.week_heading)
            Row(Modifier.fillMaxWidth()) {
                (-3L..3L).forEach { offset ->
                    val day = today.plusDays(offset)
                    val tracks = weekTracks(day)
                    WeekDay(
                        day = day,
                        tracks = tracks,
                        periodConnectPrevious = offset > -3 && tracks.period != CalendarPeriodLayer.NONE &&
                            tracks.period == weekTracks(day.minusDays(1)).period,
                        periodConnectNext = offset < 3 && tracks.period != CalendarPeriodLayer.NONE &&
                            tracks.period == weekTracks(day.plusDays(1)).period,
                        fertileConnectPrevious = offset > -3 && tracks.fertile && weekTracks(day.minusDays(1)).fertile,
                        fertileConnectNext = offset < 3 && tracks.fertile && weekTracks(day.plusDays(1)).fertile,
                        periodColor = periodColor,
                        onPeriodColor = onPeriodColor,
                        onClick = { onOpenDay(day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            OutlinedButton(onClick = { onOpenDay(today) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Tune, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.day_overview))
            }
            if (prediction.periodStarts.isNotEmpty()) {
                Text(
                    pluralStringResource(
                        R.plurals.based_on_periods,
                        prediction.periodStarts.size,
                        prediction.periodStarts.size,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            MonthlyForecastSection(state)
        }
    }
}

@Composable
private fun PhaseGuidanceCard(insight: DailyCycleInsight, onSelfCare: (() -> Unit)? = null) {
    val phase = insight.phase ?: return
    val ovulation = insight.fertilityStatus == FertilityStatus.OVULATION
    val icon = when {
        ovulation -> Icons.Outlined.WbSunny
        phase == CyclePhase.MENSTRUAL -> Icons.Outlined.WaterDrop
        phase == CyclePhase.FOLLICULAR -> Icons.Outlined.Autorenew
        phase == CyclePhase.FERTILE -> Icons.Outlined.Spa
        else -> Icons.Outlined.Timelapse
    }
    val body = when {
        ovulation -> R.string.phase_guidance_ovulation
        phase == CyclePhase.MENSTRUAL -> R.string.phase_guidance_menstrual
        phase == CyclePhase.FOLLICULAR -> R.string.phase_guidance_follicular
        phase == CyclePhase.FERTILE -> R.string.phase_guidance_fertile
        else -> R.string.phase_guidance_luteal
    }
    val feelings = when {
        ovulation -> R.string.phase_feelings_ovulation
        phase == CyclePhase.MENSTRUAL -> R.string.phase_feelings_menstrual
        phase == CyclePhase.FOLLICULAR -> R.string.phase_feelings_follicular
        phase == CyclePhase.FERTILE -> R.string.phase_feelings_fertile
        else -> R.string.phase_feelings_luteal
    }
    val phaseName = if (ovulation) R.string.phase_ovulation else phaseHeadingLabel(phase)
    val phaseColor = when {
        phase == CyclePhase.MENSTRUAL -> MaterialTheme.colorScheme.secondary
        ovulation -> MaterialTheme.colorScheme.primary
        phase == CyclePhase.FERTILE -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(phaseColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = phaseColor)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(phaseName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            SectionLabel(Icons.Outlined.MonitorHeart, R.string.phase_guidance_title, phaseColor)
            Text(stringResource(body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            SectionLabel(Icons.Outlined.SentimentSatisfied, R.string.phase_feelings_title, phaseColor)
            Text(stringResource(feelings), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (phase == CyclePhase.MENSTRUAL || phase == CyclePhase.LUTEAL) {
                Text(
                    stringResource(R.string.phase_guidance_seek_help),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (phase == CyclePhase.MENSTRUAL && onSelfCare != null) {
                TextButton(onClick = onSelfCare) {
                    Icon(Icons.Outlined.Healing, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.self_care_title))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SelfCareSheet(onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf<SelfCareActivity?>(null) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var targetMillis by remember { mutableStateOf<Long?>(null) }
    val running = targetMillis != null && remainingSeconds > 0
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(targetMillis) {
        val target = targetMillis ?: return@LaunchedEffect
        while (true) {
            val remaining = SelfCareTimer.remainingSeconds(target, SystemClock.elapsedRealtime())
            remainingSeconds = remaining
            if (remaining == 0) {
                targetMillis = null
                break
            }
            delay(250)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        sheetGesturesEnabled = false,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(R.string.self_care_title, onDismiss)
            if (selected == null) {
                Text(stringResource(R.string.self_care_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelfCareActivity.entries.forEach { activity ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(activity.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(activity.title), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.self_care_minutes, activity.minutes),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = {
                            selected = activity
                            remainingSeconds = activity.minutes * 60
                            targetMillis = SystemClock.elapsedRealtime() + remainingSeconds * 1_000L
                        }) { Text(stringResource(R.string.self_care_start)) }
                    }
                }
            } else {
                val activity = requireNotNull(selected)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(activity.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(activity.title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Text(stringResource(activity.instructions), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    String.format(Locale.ROOT, "%d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (running) {
                                remainingSeconds = SelfCareTimer.remainingSeconds(
                                    requireNotNull(targetMillis),
                                    SystemClock.elapsedRealtime(),
                                )
                                targetMillis = null
                            } else {
                                if (remainingSeconds == 0) remainingSeconds = activity.minutes * 60
                                targetMillis = SystemClock.elapsedRealtime() + remainingSeconds * 1_000L
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(if (running) R.string.self_care_pause else R.string.self_care_resume))
                    }
                    OutlinedButton(
                        onClick = {
                            targetMillis = null
                            selected = null
                            remainingSeconds = 0
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.self_care_stop)) }
                }
            }
            Text(
                stringResource(R.string.self_care_safety),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun UpcomingCycleSection(
    insight: DailyCycleInsight,
    predictionsEnabled: Boolean,
    dateFormat: DateTimeFormatter,
    distance: Int?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(Icons.Outlined.EventAvailable, R.string.upcoming_cycle)
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp),
        ) {
            val nextText = when {
                !predictionsEnabled -> stringResource(R.string.predictions_disabled)
                insight.nextPeriodStart == null -> stringResource(R.string.no_period_data)
                distance == null -> insight.nextPeriodStart.format(dateFormat)
                distance > 0 -> stringResource(
                    R.string.date_with_relative,
                    insight.nextPeriodStart.format(dateFormat),
                    pluralStringResource(R.plurals.days_until_period, distance, distance),
                )
                distance == 0 -> stringResource(R.string.predicted_today)
                else -> insight.nextPeriodStart.format(dateFormat)
            }
            UpcomingCycleRow(Icons.Outlined.WaterDrop, R.string.next_period, nextText)
            insight.fertility?.let { fertility ->
                HorizontalDivider()
                UpcomingCycleRow(
                    Icons.Outlined.Spa,
                    R.string.fertile_legend,
                    stringResource(
                        R.string.estimated_window,
                        fertility.fertileStart.format(dateFormat),
                        fertility.fertileEnd.format(dateFormat),
                    ),
                )
                HorizontalDivider()
                UpcomingCycleRow(
                    Icons.Outlined.WbSunny,
                    R.string.ovulation_legend,
                    fertility.ovulation.format(dateFormat),
                )
            }
        }
        if (insight.fertility != null) Text(
            stringResource(R.string.fertility_estimate_notice),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun UpcomingCycleRow(icon: ImageVector, @StringRes label: Int, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(label), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MonthlyForecastSection(state: AppState) {
    val locale = currentLocale()
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(Icons.Outlined.EventAvailable, R.string.forecast_heading)
        if (!state.backup.settings.canPredictPeriods) {
            Text(
                stringResource(profileNotice(state.backup.settings.profile.lifeSituation)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp),
        ) {
            state.prediction.monthlyForecasts.forEachIndexed { index, forecast ->
                if (index > 0) HorizontalDivider()
                val snapshot = state.forecastSnapshots[forecast.month]
                val icon = if (forecast.status == ForecastStatus.RECORDED) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.EventAvailable
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
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
                        if (forecast.status == ForecastStatus.RECORDED) Text(
                            stringResource(R.string.forecast_recorded, forecast.start?.format(dateFormat).orEmpty()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (snapshot != null) {
                            Text(
                                stringResource(
                                    if (snapshot.reconstructed) R.string.forecast_reconstructed else R.string.forecast_saved,
                                    snapshot.earliestStart.format(dateFormat),
                                    snapshot.latestStart.format(dateFormat),
                                ),
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            CycleAnalysis.closestRecordedStart(snapshot, state.prediction.periodStarts)?.let { actual ->
                                val difference = ChronoUnit.DAYS.between(snapshot.periodStart, actual).toInt()
                                Text(
                                    stringResource(
                                        R.string.forecast_difference,
                                        if (difference > 0) "+$difference" else difference.toString(),
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        } else if (forecast.status != ForecastStatus.RECORDED) Text(
                            when (forecast.status) {
                                ForecastStatus.ESTIMATED -> stringResource(
                                    R.string.forecast_estimated,
                                    forecast.earliestStart?.format(dateFormat).orEmpty(),
                                    forecast.latestStart?.format(dateFormat).orEmpty(),
                                )
                                ForecastStatus.NOT_EXPECTED -> stringResource(R.string.forecast_not_expected)
                                ForecastStatus.UNAVAILABLE -> stringResource(R.string.forecast_unavailable)
                                ForecastStatus.RECORDED -> ""
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@StringRes
private fun phaseHeadingLabel(phase: CyclePhase): Int = when (phase) {
    CyclePhase.MENSTRUAL -> R.string.phase_heading_menstrual
    CyclePhase.FOLLICULAR -> R.string.phase_heading_follicular
    CyclePhase.FERTILE -> R.string.phase_heading_fertile
    CyclePhase.LUTEAL -> R.string.phase_heading_luteal
}

@Composable
private fun WeekDay(
    day: LocalDate,
    tracks: CalendarDayTracks,
    periodConnectPrevious: Boolean,
    periodConnectNext: Boolean,
    fertileConnectPrevious: Boolean,
    fertileConnectNext: Boolean,
    periodColor: Color,
    onPeriodColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val periodBackground = when (tracks.period) {
        CalendarPeriodLayer.RECORDED -> periodColor
        CalendarPeriodLayer.PREDICTED -> MaterialTheme.colorScheme.secondaryContainer
        CalendarPeriodLayer.NONE -> Color.Transparent
    }
    val foreground = when (tracks.period) {
        CalendarPeriodLayer.RECORDED -> onPeriodColor
        CalendarPeriodLayer.PREDICTED -> MaterialTheme.colorScheme.onSecondaryContainer
        CalendarPeriodLayer.NONE -> if (tracks.fertile) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else MaterialTheme.colorScheme.onSurface
    }
    val periodShape = RoundedCornerShape(
        topStartPercent = if (periodConnectPrevious) 0 else 50,
        bottomStartPercent = if (periodConnectPrevious) 0 else 50,
        topEndPercent = if (periodConnectNext) 0 else 50,
        bottomEndPercent = if (periodConnectNext) 0 else 50,
    )
    val fertileShape = RoundedCornerShape(
        topStartPercent = if (fertileConnectPrevious) 0 else 50,
        bottomStartPercent = if (fertileConnectPrevious) 0 else 50,
        topEndPercent = if (fertileConnectNext) 0 else 50,
        bottomEndPercent = if (fertileConnectNext) 0 else 50,
    )
    val labels = buildList {
        when (tracks.period) {
            CalendarPeriodLayer.RECORDED -> add(stringResource(R.string.recorded_legend))
            CalendarPeriodLayer.PREDICTED -> add(stringResource(R.string.predicted_legend))
            CalendarPeriodLayer.NONE -> Unit
        }
        if (tracks.fertile) add(stringResource(R.string.fertile_legend))
        if (tracks.ovulation) add(stringResource(R.string.ovulation_legend))
    }
    val description = (listOf(
        day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)),
    ) + labels).joinToString(", ")
    Box(
        modifier = modifier.height(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (tracks.fertile) Box(
            Modifier.fillMaxWidth().height(48.dp).padding(
                start = if (fertileConnectPrevious) 0.dp else 2.dp,
                end = if (fertileConnectNext) 0.dp else 2.dp,
            ).clip(fertileShape).background(MaterialTheme.colorScheme.tertiaryContainer),
        )
        if (tracks.period != CalendarPeriodLayer.NONE) Box(
            Modifier.fillMaxWidth().height(38.dp).padding(
                start = if (periodConnectPrevious) 0.dp else 2.dp,
                end = if (periodConnectNext) 0.dp else 2.dp,
            ).clip(periodShape).background(periodBackground),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale), color = foreground, style = MaterialTheme.typography.labelSmall)
            Box(
                Modifier.size(26.dp).clip(CircleShape)
                    .then(if (tracks.ovulation) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                    .then(if (day == LocalDate.now()) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    day.dayOfMonth.toString(),
                    color = if (tracks.ovulation) MaterialTheme.colorScheme.onPrimaryContainer else foreground,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick)
                .semantics { contentDescription = description },
        )
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
    val pagerState = rememberPagerState(
        initialPage = CalendarPaging.pageFor(YearMonth.now()),
        pageCount = { CalendarPaging.pageCount },
    )
    val scope = rememberCoroutineScope()
    var overviewExpanded by remember { mutableStateOf(false) }
    val recorded = state.backup.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day)
    val predicted = state.periodEstimates.flatMap { estimate ->
        generateSequence(estimate.start) { it.plusDays(1) }.takeWhile { it < estimate.endExclusive }.toList()
    }.toSet()
    val fertility = CycleInsights.fertilityEstimates(state.backup, state.forecastSnapshots)
    val fertile = fertility.flatMap { estimate ->
        generateSequence(estimate.fertileStart) { it.plusDays(1) }.takeWhile { !it.isAfter(estimate.fertileEnd) }.toList()
    }.toSet()
    val ovulation = fertility.mapTo(mutableSetOf(), FertilityEstimate::ovulation)
    val tracksFor: (LocalDate) -> CalendarDayTracks = { day ->
        calendarDayTracks(day, recorded, predicted, fertile, ovulation)
    }
    val periodColor = calendarPeriodRgb(state.backup.settings.palette, state.backup.settings.customPalette).color()
    val onPeriodColor = periodColor.contrastColor()
    val entryColor = calendarEntryRgb(state.backup.settings.palette, state.backup.settings.customPalette).color()
    LaunchedEffect(pagerState.settledPage) { overviewExpanded = false }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.calendar_heading),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { onEdit(LocalDate.now()) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_entry))
            }
        }
        HorizontalPager(
            state = pagerState,
            key = { it },
            modifier = Modifier.weight(1f),
        ) { page ->
            CalendarMonthPage(
                state = state,
                shownMonth = CalendarPaging.monthFor(page),
                overviewExpanded = overviewExpanded,
                onToggleOverview = { overviewExpanded = !overviewExpanded },
                onPrevious = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
                previousEnabled = page > 0,
                nextEnabled = page < CalendarPaging.pageCount - 1,
                tracksFor = tracksFor,
                periodColor = periodColor,
                onPeriodColor = onPeriodColor,
                entryColor = entryColor,
                onDayClick = { day ->
                    val targetPage = CalendarPaging.pageFor(YearMonth.from(day))
                    if (targetPage == page) {
                        onEdit(day)
                    } else {
                        scope.launch {
                            pagerState.scrollToPage(targetPage)
                            onEdit(day)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun CalendarMonthPage(
    state: AppState,
    shownMonth: YearMonth,
    overviewExpanded: Boolean,
    onToggleOverview: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    tracksFor: (LocalDate) -> CalendarDayTracks,
    periodColor: Color,
    onPeriodColor: Color,
    entryColor: Color,
    onDayClick: (LocalDate) -> Unit,
) {
    val locale = currentLocale()
    val firstDay = state.backup.settings.firstDayOfWeek
    val weekdays = if (firstDay == DayOfWeek.MONDAY) {
        DayOfWeek.entries
    } else {
        listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)
    }
    val days = CalendarPaging.gridDays(shownMonth, firstDay)
    var legendExpanded by remember { mutableStateOf(false) }
    val previousMonthLabel = stringResource(R.string.previous_month)
    val nextMonthLabel = stringResource(R.string.next_month)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = previousEnabled) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = previousMonthLabel)
            }
            Text(
                shownMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                    .replaceFirstChar { it.titlecase(locale) },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(onClick = onNext, enabled = nextEnabled) {
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
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEachIndexed { column, day ->
                    val tracks = tracksFor(day)
                    CalendarDay(
                        day = day,
                        tracks = tracks,
                        hasDetails = state.logsByDay[day]?.hasCalendarMarker == true,
                        inShownMonth = YearMonth.from(day) == shownMonth,
                        periodConnectPrevious = column > 0 && tracks.period != CalendarPeriodLayer.NONE &&
                            tracks.period == tracksFor(day.minusDays(1)).period,
                        periodConnectNext = column < 6 && tracks.period != CalendarPeriodLayer.NONE &&
                            tracks.period == tracksFor(day.plusDays(1)).period,
                        fertileConnectPrevious = column > 0 && tracks.fertile && tracksFor(day.minusDays(1)).fertile,
                        fertileConnectNext = column < 6 && tracks.fertile && tracksFor(day.plusDays(1)).fertile,
                        periodColor = periodColor,
                        onPeriodColor = onPeriodColor,
                        entryColor = entryColor,
                        onClick = { onDayClick(day) },
                        enabled = day in DayLog.MIN_DATE..DayLog.MAX_DATE,
                        modifier = Modifier.weight(1f).height(56.dp),
                    )
                }
            }
        }
        TextButton(
            onClick = { legendExpanded = !legendExpanded },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
        ) {
            Text(stringResource(if (legendExpanded) R.string.legend_less else R.string.legend_more))
            Spacer(Modifier.width(4.dp))
            Icon(if (legendExpanded) Icons.Outlined.Remove else Icons.Default.Add, contentDescription = null)
        }
        if (legendExpanded) {
            Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    LegendItem(
                        periodColor,
                        stringResource(R.string.recorded_legend),
                        stringResource(R.string.recorded_legend_detail),
                        Modifier.weight(1f),
                    )
                    LegendItem(
                        MaterialTheme.colorScheme.secondaryContainer,
                        stringResource(R.string.predicted_legend),
                        stringResource(R.string.predicted_legend_detail),
                        Modifier.weight(1f),
                    )
                }
                if (state.backup.settings.canEstimateFertility) {
                    Row(Modifier.fillMaxWidth()) {
                        LegendItem(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            stringResource(R.string.fertile_legend),
                            stringResource(R.string.fertile_legend_detail),
                            Modifier.weight(1f),
                        )
                        LegendItem(
                            MaterialTheme.colorScheme.primaryContainer,
                            stringResource(R.string.ovulation_legend),
                            stringResource(R.string.ovulation_legend_detail),
                            Modifier.weight(1f),
                        )
                    }
                }
                LegendItem(
                    entryColor,
                    stringResource(R.string.calendar_note_marker),
                    stringResource(R.string.calendar_note_marker_detail),
                    smallMarker = true,
                )
            }
        }
        TextButton(onClick = onToggleOverview, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Outlined.Insights, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.month_overview))
            Spacer(Modifier.width(4.dp))
            Icon(
                if (overviewExpanded) Icons.Outlined.Remove else Icons.Default.Add,
                contentDescription = null,
            )
        }
        if (overviewExpanded) MonthComparison(state, shownMonth)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CalendarDay(
    day: LocalDate,
    tracks: CalendarDayTracks,
    hasDetails: Boolean,
    inShownMonth: Boolean,
    periodConnectPrevious: Boolean,
    periodConnectNext: Boolean,
    fertileConnectPrevious: Boolean,
    fertileConnectNext: Boolean,
    periodColor: Color,
    onPeriodColor: Color,
    entryColor: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    val periodBackground = when (tracks.period) {
        CalendarPeriodLayer.RECORDED -> periodColor
        CalendarPeriodLayer.PREDICTED -> MaterialTheme.colorScheme.secondaryContainer
        CalendarPeriodLayer.NONE -> Color.Transparent
    }
    val foreground = when (tracks.period) {
        CalendarPeriodLayer.RECORDED -> onPeriodColor
        CalendarPeriodLayer.PREDICTED -> MaterialTheme.colorScheme.onSecondaryContainer
        CalendarPeriodLayer.NONE -> if (tracks.fertile) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else MaterialTheme.colorScheme.onSurface
    }
    val periodShape = RoundedCornerShape(
        topStartPercent = if (periodConnectPrevious) 0 else 50,
        bottomStartPercent = if (periodConnectPrevious) 0 else 50,
        topEndPercent = if (periodConnectNext) 0 else 50,
        bottomEndPercent = if (periodConnectNext) 0 else 50,
    )
    val fertileShape = RoundedCornerShape(
        topStartPercent = if (fertileConnectPrevious) 0 else 50,
        bottomStartPercent = if (fertileConnectPrevious) 0 else 50,
        topEndPercent = if (fertileConnectNext) 0 else 50,
        bottomEndPercent = if (fertileConnectNext) 0 else 50,
    )
    val date = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(currentLocale()))
    val labels = buildList {
        when (tracks.period) {
            CalendarPeriodLayer.RECORDED -> add(stringResource(R.string.recorded_legend))
            CalendarPeriodLayer.PREDICTED -> add(stringResource(R.string.predicted_legend))
            CalendarPeriodLayer.NONE -> Unit
        }
        if (tracks.predictedOverlap) add(stringResource(R.string.predicted_legend))
        if (tracks.fertile) add(stringResource(R.string.fertile_legend))
        if (tracks.ovulation) add(stringResource(R.string.ovulation_legend))
        if (hasDetails) add(stringResource(R.string.recorded_values))
    }
    val description = (listOf(date) + labels).joinToString(", ")
    Box(
        modifier = modifier.alpha(if (inShownMonth) 1f else 0.42f),
        contentAlignment = Alignment.Center,
    ) {
        if (tracks.fertile) Box(
            Modifier.fillMaxWidth().height(42.dp).padding(
                start = if (fertileConnectPrevious) 0.dp else 3.dp,
                end = if (fertileConnectNext) 0.dp else 3.dp,
            ).clip(fertileShape).background(MaterialTheme.colorScheme.tertiaryContainer),
        )
        if (tracks.period != CalendarPeriodLayer.NONE) Box(
            Modifier.fillMaxWidth().height(32.dp).padding(
                start = if (periodConnectPrevious) 0.dp else 3.dp,
                end = if (periodConnectNext) 0.dp else 3.dp,
            ).clip(periodShape).background(periodBackground),
        ) {
            if (tracks.predictedOverlap) {
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).width(20.dp).height(3.dp)
                        .clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                )
            }
        }
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .then(if (tracks.ovulation) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                .then(if (day == LocalDate.now()) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(),
                color = if (tracks.ovulation) MaterialTheme.colorScheme.onPrimaryContainer else foreground,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (hasDetails) Box(
            Modifier.align(Alignment.TopEnd).padding(5.dp).size(6.dp).clip(CircleShape)
                .background(entryColor),
        )
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick)
                .semantics { contentDescription = description },
        )
    }
}

@Composable
private fun MonthComparison(state: AppState, month: YearMonth) {
    val snapshot = state.forecastSnapshots[month]
    val actual = snapshot?.let { CycleAnalysis.closestRecordedStart(it, state.prediction.periodStarts) }
        ?: state.prediction.periodStarts.lastOrNull { YearMonth.from(it) == month }
    val estimate = state.periodEstimates.firstOrNull { YearMonth.from(it.start) == month }
    val fertility = if (month == YearMonth.now()) {
        state.todayInsight.fertility
    } else {
        CycleInsights.fertilityEstimates(state.backup, state.forecastSnapshots)
            .firstOrNull { YearMonth.from(it.periodStart) == month }
    }
    if (actual == null && estimate == null && fertility == null) return
    val locale = currentLocale()
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val estimateText = snapshot?.let {
        stringResource(R.string.estimated_window, it.earliestStart.format(dateFormat), it.latestStart.format(dateFormat))
    } ?: estimate?.let {
        stringResource(
            R.string.estimated_window,
            (it.earliestStart ?: it.start).format(dateFormat),
            (it.latestStart ?: it.start).format(dateFormat),
        )
    } ?: "—"
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MonthMetric(
                Icons.Outlined.WaterDrop,
                R.string.recorded_legend,
                actual?.format(dateFormat) ?: "—",
                MaterialTheme.colorScheme.secondary,
                Modifier.weight(1f),
            )
            MonthMetric(
                Icons.Outlined.EventRepeat,
                R.string.predicted_legend,
                estimateText,
                MaterialTheme.colorScheme.secondary,
                Modifier.weight(1f),
            )
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MonthMetric(
                Icons.Outlined.Spa,
                R.string.fertile_legend,
                fertility?.let {
                    stringResource(
                        R.string.estimated_window,
                        it.fertileStart.format(dateFormat),
                        it.fertileEnd.format(dateFormat),
                    )
                } ?: "—",
                MaterialTheme.colorScheme.tertiary,
                Modifier.weight(1f),
            )
            MonthMetric(
                Icons.Outlined.WbSunny,
                R.string.ovulation_legend,
                fertility?.ovulation?.format(dateFormat) ?: "—",
                MaterialTheme.colorScheme.primary,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthMetric(
    icon: ImageVector,
    @StringRes label: Int,
    value: String,
    tint: Color,
    modifier: Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(label), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LegendItem(
    background: Color,
    label: String,
    description: String?,
    modifier: Modifier = Modifier,
    smallMarker: Boolean = false,
) {
    Row(modifier, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.padding(top = if (smallMarker) 8.dp else 3.dp)
                .size(if (smallMarker) 7.dp else 18.dp).clip(CircleShape).background(background),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryScreen(state: AppState) {
    val locale = currentLocale()
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val prediction = state.prediction
    val today = LocalDate.now()
    val pastStarts = prediction.periodStarts.filter { !it.isAfter(today) }
    val futureStarts = prediction.periodStarts.filter { it.isAfter(today) }
    val cycleHistory = CycleAnalysis.recentHistory(
        periodStarts = pastStarts,
        bleedingDays = state.backup.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day),
        lutealPhaseDays = state.backup.settings.lutealPhaseLength,
    )
    val predictionAccuracy = CycleAnalysis.predictionAccuracy(pastStarts, state.forecastSnapshots)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.history_heading), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(Icons.Outlined.Autorenew, R.string.average_cycle, pluralStringResource(R.plurals.days_value, prediction.averageCycleLength, prediction.averageCycleLength), Modifier.weight(1f))
            Metric(Icons.Outlined.WaterDrop, R.string.average_period, pluralStringResource(R.plurals.days_value, prediction.averagePeriodLength, prediction.averagePeriodLength), Modifier.weight(1f))
            Metric(Icons.Outlined.History, R.string.recorded_cycles, pastStarts.size.toString(), Modifier.weight(1f))
        }
        predictionAccuracy?.let { PredictionAccuracyCard(it) }
        CycleLengthChart(pastStarts, locale)
        if (state.backup.settings.canEstimateFertility) CycleHistoryDetails(cycleHistory, locale)
        if (futureStarts.isNotEmpty()) {
            SectionLabel(Icons.Outlined.EventRepeat, R.string.upcoming_entries)
            futureStarts.forEach { start ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(start.format(dateFormat), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.period_recorded), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        HorizontalDivider()
        if (pastStarts.isEmpty()) {
            Text(stringResource(R.string.no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            pastStarts.asReversed().forEach { start ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
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
private fun PredictionAccuracyCard(summary: PredictionAccuracySummary) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.prediction_accuracy_summary), fontWeight = FontWeight.SemiBold)
            Text(
                pluralStringResource(
                    R.plurals.average_prediction_error,
                    summary.averageErrorDays,
                    summary.averageErrorDays,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(
                    R.string.within_saved_window,
                    summary.withinWindowCount,
                    summary.sampleCount,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CycleHistoryDetails(samples: List<CycleHistorySample>, locale: Locale) {
    if (samples.isEmpty()) return
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionLabel(Icons.Outlined.Timelapse, R.string.recent_cycle_details)
        samples.asReversed().forEachIndexed { index, sample ->
            val periodText = pluralStringResource(
                R.plurals.period_length_value,
                sample.periodDays,
                sample.periodDays,
            )
            val ovulationText = stringResource(R.string.estimated_ovulation, sample.ovulation.format(dateFormat))
            val fertileText = stringResource(
                R.string.fertile_window_value,
                sample.fertileStart.format(dateFormat),
                sample.fertileEnd.format(dateFormat),
            )
            val description = listOf(
                sample.start.format(dateFormat),
                pluralStringResource(R.plurals.days_value, sample.cycleDays, sample.cycleDays),
                periodText,
                ovulationText,
                fertileText,
            ).joinToString(", ")
            Column(
                Modifier.fillMaxWidth().semantics { contentDescription = description },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(sample.start.format(dateFormat), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        pluralStringResource(R.plurals.days_value, sample.cycleDays, sample.cycleDays),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val baseColor = MaterialTheme.colorScheme.outlineVariant
                val periodColor = MaterialTheme.colorScheme.secondary
                val fertileColor = MaterialTheme.colorScheme.tertiaryContainer
                val ovulationColor = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                    val centerY = size.height / 2f
                    val periodEnd = size.width * sample.periodDays.coerceIn(0, sample.cycleDays) / sample.cycleDays
                    val fertileStart = size.width * ChronoUnit.DAYS.between(sample.start, sample.fertileStart)
                        .toFloat().div(sample.cycleDays).coerceIn(0f, 1f)
                    val fertileEnd = size.width * ChronoUnit.DAYS.between(sample.start, sample.fertileEnd.plusDays(1))
                        .toFloat().div(sample.cycleDays).coerceIn(0f, 1f)
                    val ovulation = size.width * ChronoUnit.DAYS.between(sample.start, sample.ovulation)
                        .toFloat().div(sample.cycleDays).coerceIn(0f, 1f)
                    drawLine(baseColor, Offset(0f, centerY), Offset(size.width, centerY), size.height, StrokeCap.Round)
                    if (periodEnd > 0f) {
                        drawLine(periodColor, Offset(0f, centerY), Offset(periodEnd, centerY), size.height, StrokeCap.Round)
                    }
                    drawLine(
                        fertileColor,
                        Offset(fertileStart, centerY),
                        Offset(fertileEnd, centerY),
                        size.height * 0.72f,
                        StrokeCap.Round,
                    )
                    drawCircle(ovulationColor, radius = size.height / 2f, center = Offset(ovulation, centerY))
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.WaterDrop, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text(periodText, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text(ovulationText, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(fertileText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (index < samples.lastIndex) HorizontalDivider()
        }
    }
}

@Composable
private fun CycleLengthChart(periodStarts: List<LocalDate>, locale: Locale) {
    val cycles = CycleAnalysis.recentLengths(periodStarts)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel(Icons.Outlined.Insights, R.string.cycle_analysis)
        if (cycles.size < 2) {
            Text(stringResource(R.string.more_history_needed), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val monthFormat = remember(locale) { DateTimeFormatter.ofPattern("MMM yy", locale) }
            val lineColor = MaterialTheme.colorScheme.primary
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val min = cycles.minOf(CycleLengthSample::days) - 1
                    val max = cycles.maxOf(CycleLengthSample::days) + 1
                    val points = cycles.mapIndexed { index, sample ->
                        val x = size.width * (index + 0.5f) / cycles.size
                        val ratio = (sample.days - min).toFloat() / (max - min)
                        Offset(x, size.height - ratio * size.height)
                    }
                    points.zipWithNext().forEach { (first, second) ->
                        drawLine(lineColor, first, second, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                    }
                    points.forEach { drawCircle(lineColor, radius = 5.dp.toPx(), center = it) }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    cycles.forEach { sample ->
                        val month = sample.start.format(monthFormat)
                        val description = stringResource(R.string.cycle_length_description, month, sample.days)
                        Column(
                            modifier = Modifier.weight(1f).semantics {
                                contentDescription = description
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                sample.days.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = lineColor,
                            )
                            Text(month, style = MaterialTheme.typography.labelSmall)
                        }
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
    onMyCalendarImport: () -> Unit,
    onMyCalendarExport: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onCalendarSelect: (Long) -> Unit,
    onCalendarDisconnect: () -> Unit,
) {
    val settings = state.backup.settings
    val context = LocalContext.current
    var pageName by rememberSaveable { mutableStateOf<String?>(null) }
    val page = pageName?.let(SettingsPage::valueOf)
    val scrollState = rememberScrollState()
    BackHandler(enabled = page != null) { pageName = null }
    LaunchedEffect(pageName) { scrollState.scrollTo(0) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp),
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
                    SwitchRow(R.string.predictions, settings.predictionsEnabled, Icons.Outlined.Insights) {
                        onSave(settings.copy(predictionsEnabled = it))
                    }
                    SwitchRow(R.string.simple_mode, settings.simpleMode, Icons.Outlined.VisibilityOff) {
                        onSave(settings.copy(simpleMode = it))
                    }
                    Text(
                        stringResource(R.string.simple_mode_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Stepper(
                        R.string.default_cycle_length,
                        settings.cycleLengthOverride ?: state.prediction.averageCycleLength,
                        15..90,
                        Icons.Outlined.Autorenew,
                    ) {
                        onSave(settings.copy(cycleLength = it, cycleLengthOverride = it))
                    }
                    Stepper(
                        R.string.default_period_length,
                        settings.periodLengthOverride ?: state.prediction.averagePeriodLength,
                        1..14,
                        Icons.Outlined.WaterDrop,
                    ) {
                        onSave(settings.copy(periodLength = it, periodLengthOverride = it))
                    }
                    if (settings.cycleLengthOverride != null || settings.periodLengthOverride != null) {
                        TextButton(onClick = {
                            onSave(settings.copy(cycleLengthOverride = null, periodLengthOverride = null))
                        }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.use_automatic_estimate))
                        }
                    }
                    ChoiceRow(
                        label = R.string.first_day_of_week,
                        choices = listOf(
                            ChoiceOption(DayOfWeek.MONDAY, R.string.monday),
                            ChoiceOption(DayOfWeek.SUNDAY, R.string.sunday),
                        ),
                        selected = settings.firstDayOfWeek,
                        icon = Icons.Outlined.CalendarMonth,
                    ) { onSave(settings.copy(firstDayOfWeek = it)) }
                    InfoBlock(R.string.daily_measurements, R.string.daily_measurements_body, Icons.Outlined.MonitorHeart)
                }
                SettingsPage.PROFILE -> ProfileSettings(settings, onSave)
                SettingsPage.HOME -> {
                    SwitchRow(
                        R.string.home_show_phase_guidance,
                        settings.showPhaseGuidance,
                        Icons.Outlined.Autorenew,
                    ) { onSave(settings.copy(showPhaseGuidance = it)) }
                    SwitchRow(
                        R.string.home_show_self_care,
                        settings.showSelfCare,
                        Icons.Outlined.Healing,
                    ) { onSave(settings.copy(showSelfCare = it)) }
                    SwitchRow(
                        R.string.home_show_cycle_details,
                        settings.showCycleDetails,
                        Icons.Outlined.Insights,
                    ) { onSave(settings.copy(showCycleDetails = it)) }
                }
                SettingsPage.APPEARANCE -> {
                    SectionLabel(Icons.Outlined.Palette, R.string.theme)
                    AppTheme.entries.forEach { theme ->
                        AppearancePreviewRow(
                            label = themeLabel(theme),
                            icon = themeIcon(theme),
                            selected = settings.theme == theme,
                        ) { onSave(settings.copy(theme = theme)) }
                    }
                    SectionLabel(Icons.Outlined.Palette, R.string.color_palette)
                    AppPalette.entries.forEach { palette ->
                        AppearancePreviewRow(
                            label = paletteLabel(palette),
                            colors = if (palette == AppPalette.CUSTOM) emptyList() else {
                                palettePreviewColors(palette, settings.customPalette)
                            },
                            icon = Icons.Outlined.Edit.takeIf { palette == AppPalette.CUSTOM },
                            summary = R.string.palette_custom_hint.takeIf { palette == AppPalette.CUSTOM },
                            selected = settings.palette == palette,
                        ) { onSave(settings.copy(palette = palette)) }
                    }
                    if (settings.palette == AppPalette.CUSTOM) CustomPaletteSettings(settings, onSave)
                    LanguageRow()
                }
                SettingsPage.REMINDERS -> {
                    SwitchRow(
                        R.string.period_reminder,
                        settings.reminderEnabled,
                        Icons.Outlined.NotificationsNone,
                        onChange = onReminderChange,
                    )
                    if (settings.reminderEnabled) {
                        Stepper(R.string.remind_before, settings.reminderDays, 0..14, Icons.Outlined.NotificationsNone) {
                            onSave(settings.copy(reminderDays = it))
                        }
                    }
                }
                SettingsPage.CALENDAR -> CalendarSyncSettings(
                    state = state,
                    onSave = onSave,
                    onRequestPermission = onRequestCalendarPermission,
                    onSelect = onCalendarSelect,
                    onDisconnect = onCalendarDisconnect,
                )
                SettingsPage.DATA -> {
                    InfoBlock(R.string.device_transfer, R.string.device_transfer_body, Icons.Outlined.Devices)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    InfoBlock(R.string.my_calendar_import, R.string.my_calendar_import_body, Icons.Outlined.CalendarMonth)
                    Text(
                        stringResource(R.string.my_calendar_export_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = onMyCalendarExport,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.busy && state.backup.logs.isNotEmpty(),
                    ) {
                        Icon(Icons.Outlined.ImportExport, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.my_calendar_export_action))
                    }
                    OutlinedButton(onClick = onMyCalendarImport, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
                        Icon(Icons.Outlined.ImportExport, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.my_calendar_import_action))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    OutlinedButton(onClick = onDeleteAll, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_all_data), color = MaterialTheme.colorScheme.error)
                    }
                }
                SettingsPage.PRIVACY -> {
                    SettingsLink(Icons.Outlined.VerifiedUser, R.string.privacy_policy) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri()))
                    }
                    SettingsLink(Icons.Outlined.Security, R.string.privacy) { onInfo(InfoDialog.PRIVACY) }
                    SettingsLink(Icons.Outlined.MonitorHeart, R.string.about_cycle) { onInfo(InfoDialog.CYCLE) }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SupportButton()
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MyCalendarPreviewDialog(
    preview: MyCalendarPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val locale = currentLocale()
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.my_calendar_preview_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pluralStringResource(
                    R.plurals.my_calendar_preview_body,
                    preview.logs.size,
                    preview.logs.size,
                    preview.firstDay.format(formatter),
                    preview.lastDay.format(formatter),
                ))
                if (preview.unsupportedDetails > 0) {
                    Text(pluralStringResource(
                        R.plurals.my_calendar_unsupported_details,
                        preview.unsupportedDetails,
                        preview.unsupportedDetails,
                    ))
                }
                Text(stringResource(R.string.my_calendar_merge_notice), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.merge_import)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ProfileSettings(settings: AppSettings, onSave: (AppSettings) -> Unit) {
    val profile = settings.profile
    var age by remember(profile.age) { mutableStateOf(profile.age?.toString().orEmpty()) }
    var height by remember(profile.heightCm) { mutableStateOf(profile.heightCm?.toString().orEmpty()) }
    var weight by remember(profile.weightKg) { mutableStateOf(profile.weightKg?.toString().orEmpty()) }
    val ageValue = age.toIntOrNull()
    val heightValue = height.toIntOrNull()
    val weightValue = parseDecimal(weight)
    val ageValid = age.isBlank() || ageValue != null && ageValue in UserProfile.MIN_AGE..UserProfile.MAX_AGE
    val heightValid = height.isBlank() || heightValue != null && heightValue in UserProfile.MIN_HEIGHT_CM..UserProfile.MAX_HEIGHT_CM
    val weightValid = weight.isBlank() || weightValue != null && weightValue in DayLog.MIN_WEIGHT_KG..DayLog.MAX_WEIGHT_KG

    ChoiceRow(
        label = R.string.profile_goal,
        choices = listOf(
            ChoiceOption(TrackingGoal.TRACK_CYCLE, R.string.goal_track_cycle, Icons.Outlined.Autorenew),
            ChoiceOption(TrackingGoal.TRYING_TO_CONCEIVE, R.string.goal_trying_to_conceive, Icons.Outlined.ChildFriendly),
            ChoiceOption(TrackingGoal.AVOID_PREGNANCY, R.string.goal_avoid_pregnancy, Icons.Outlined.Shield),
        ),
        selected = profile.goal,
        icon = Icons.Outlined.FavoriteBorder,
    ) { onSave(settings.copy(profile = profile.copy(goal = it))) }
    ChoiceRow(
        label = R.string.life_situation,
        choices = listOf(
            ChoiceOption(LifeSituation.REGULAR_CYCLES, R.string.situation_regular, Icons.Outlined.Autorenew),
            ChoiceOption(LifeSituation.PREGNANT, R.string.situation_pregnant, Icons.Outlined.PregnantWoman),
            ChoiceOption(
                LifeSituation.HORMONAL_CONTRACEPTION,
                R.string.situation_hormonal_contraception,
                Icons.Outlined.Medication,
            ),
            ChoiceOption(LifeSituation.PERIMENOPAUSE, R.string.situation_perimenopause, Icons.Outlined.ChangeCircle),
            ChoiceOption(LifeSituation.MENOPAUSE, R.string.situation_menopause, Icons.Outlined.WbSunny),
        ),
        selected = profile.lifeSituation,
        icon = Icons.Outlined.HealthAndSafety,
    ) { onSave(settings.copy(profile = profile.copy(lifeSituation = it))) }
    ProfileNumberField(age, {
        age = it
        if (it.isBlank() || it.toIntOrNull()?.let { value -> value in UserProfile.MIN_AGE..UserProfile.MAX_AGE } == true) {
            onSave(settings.copy(profile = profile.copy(age = it.toIntOrNull())))
        }
    }, R.string.profile_age, R.string.profile_age_example, ageValid, decimal = false, icon = Icons.Outlined.Cake)
    ProfileNumberField(height, {
        height = it
        if (it.isBlank() || it.toIntOrNull()?.let { value -> value in UserProfile.MIN_HEIGHT_CM..UserProfile.MAX_HEIGHT_CM } == true) {
            onSave(settings.copy(profile = profile.copy(heightCm = it.toIntOrNull())))
        }
    }, R.string.profile_height, R.string.profile_height_example, heightValid, decimal = false, icon = Icons.Outlined.Height)
    ProfileNumberField(weight, {
        weight = it
        val parsed = parseDecimal(it)
        if (it.isBlank() || parsed != null && parsed in DayLog.MIN_WEIGHT_KG..DayLog.MAX_WEIGHT_KG) {
            onSave(settings.copy(profile = profile.copy(weightKg = parsed)))
        }
    }, R.string.profile_weight, R.string.profile_weight_example, weightValid, decimal = true, icon = Icons.Outlined.MonitorWeight)
    Stepper(
        R.string.luteal_phase_length,
        settings.lutealPhaseLength,
        7..19,
        Icons.Outlined.Timelapse,
    ) { onSave(settings.copy(lutealPhaseLength = it)) }
    InfoBlock(R.string.settings_profile, R.string.profile_context_notice, Icons.Outlined.VerifiedUser)
    Text(
        stringResource(profileNotice(profile.lifeSituation)),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (profile.goal == TrackingGoal.AVOID_PREGNANCY) {
        Text(stringResource(R.string.profile_avoid_warning), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    @StringRes placeholder: Int,
    valid: Boolean,
    decimal: Boolean,
    icon: ImageVector,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= 8) onValueChange(input.filter { it.isDigit() || decimal && (it == '.' || it == ',') })
        },
        label = { Text(stringResource(label)) },
        placeholder = { Text(stringResource(placeholder)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        supportingText = if (valid) null else {{ Text(stringResource(R.string.invalid_measurement)) }},
        isError = !valid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@StringRes
private fun profileNotice(situation: LifeSituation): Int = when (situation) {
    LifeSituation.REGULAR_CYCLES -> R.string.profile_notice_regular
    LifeSituation.PREGNANT -> R.string.profile_notice_pregnant
    LifeSituation.HORMONAL_CONTRACEPTION -> R.string.profile_notice_contraception
    LifeSituation.PERIMENOPAUSE -> R.string.profile_notice_perimenopause
    LifeSituation.MENOPAUSE -> R.string.profile_notice_menopause
}

@StringRes
private fun lifeSituationLabel(situation: LifeSituation): Int = when (situation) {
    LifeSituation.REGULAR_CYCLES -> R.string.situation_regular
    LifeSituation.PREGNANT -> R.string.situation_pregnant
    LifeSituation.HORMONAL_CONTRACEPTION -> R.string.situation_hormonal_contraception
    LifeSituation.PERIMENOPAUSE -> R.string.situation_perimenopause
    LifeSituation.MENOPAUSE -> R.string.situation_menopause
}

@Composable
private fun CalendarSyncSettings(
    state: AppState,
    onSave: (AppSettings) -> Unit,
    onRequestPermission: () -> Unit,
    onSelect: (Long) -> Unit,
    onDisconnect: () -> Unit,
) {
    val selected = state.deviceCalendars.firstOrNull { it.id == state.selectedCalendarId }
    var choosingCalendar by remember { mutableStateOf(false) }
    val syncEnabled = state.selectedCalendarId != null || choosingCalendar
    InfoBlock(R.string.calendar_sync, R.string.calendar_sync_body, Icons.Outlined.EventRepeat)
    SwitchRow(
        R.string.calendar_sync_enabled,
        syncEnabled,
        Icons.Outlined.EventRepeat,
    ) { enabled ->
        if (enabled) {
            if (state.calendarPermissionGranted) choosingCalendar = true else onRequestPermission()
        } else {
            choosingCalendar = false
            onDisconnect()
        }
    }
    SwitchRow(
        R.string.partner_view,
        state.backup.settings.partnerViewEnabled,
        Icons.Outlined.FavoriteBorder,
        enabled = state.selectedCalendarId != null,
    ) { onSave(state.backup.settings.copy(partnerViewEnabled = it)) }
    Text(
        stringResource(R.string.partner_view_body),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    when {
        !state.calendarPermissionGranted -> Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy,
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.calendar_choose))
        }
        state.deviceCalendars.isEmpty() -> Text(
            stringResource(R.string.calendar_none),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> {
            if (state.selectedCalendarId != null) {
                Text(
                    selected?.let { stringResource(R.string.calendar_active, it.displayName) }
                        ?: stringResource(R.string.calendar_unavailable),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            state.deviceCalendars.forEach { calendar ->
                DeviceCalendarRow(
                    calendar = calendar,
                    selected = calendar.id == state.selectedCalendarId,
                    enabled = !state.busy,
                    onClick = { onSelect(calendar.id) },
                )
            }
            if (state.selectedCalendarId != null) {
                OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
                    Text(stringResource(R.string.calendar_disconnect))
                }
            }
        }
    }
    Text(
        stringResource(R.string.calendar_sync_notice),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun DeviceCalendarRow(
    calendar: DeviceCalendar,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(calendar.color)))
        Column(Modifier.weight(1f)) {
            Text(calendar.displayName, fontWeight = FontWeight.SemiBold)
            if (calendar.accountName.isNotBlank() && calendar.accountName != calendar.displayName) {
                Text(calendar.accountName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (selected) Icon(Icons.Outlined.CheckCircle, contentDescription = stringResource(R.string.calendar_selected))
    }
}

@Composable
private fun SettingsCategoryRow(icon: ImageVector, @StringRes title: Int, @StringRes summary: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(summary), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, @StringRes text: Int, tint: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(stringResource(text), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SheetHeader(@StringRes title: Int, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close))
        }
    }
}

@Composable
private fun SwitchRow(
    @StringRes label: Int,
    checked: Boolean,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        icon?.let {
            Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            stringResource(label),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
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
    choices: List<ChoiceOption<T>>,
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
            choices.forEach { choice ->
                FilterChip(
                    selected = choice.value == selected,
                    onClick = { onSelect(choice.value) },
                    label = { Text(stringResource(choice.label)) },
                    leadingIcon = choice.icon?.let { choiceIcon ->
                        { Icon(choiceIcon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppearancePreviewRow(
    @StringRes label: Int,
    colors: List<Color> = emptyList(),
    icon: ImageVector? = null,
    @StringRes summary: Int? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colors.forEach { color -> Box(Modifier.size(24.dp).clip(CircleShape).background(color)) }
            }
        } else {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(label), fontWeight = FontWeight.SemiBold)
            summary?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (selected) Icon(Icons.Outlined.CheckCircle, contentDescription = stringResource(R.string.calendar_selected))
    }
}

@Composable
private fun CustomPaletteSettings(settings: AppSettings, onSave: (AppSettings) -> Unit) {
    val custom = settings.customPalette
    var target by remember { mutableStateOf<CustomColorTarget?>(null) }
    var showHex by remember { mutableStateOf(false) }
    var primary by remember(custom.primaryRgb) { mutableStateOf(custom.primaryRgb.rgbHex()) }
    var secondary by remember(custom.secondaryRgb) { mutableStateOf(custom.secondaryRgb.rgbHex()) }
    var tertiary by remember(custom.tertiaryRgb) { mutableStateOf(custom.tertiaryRgb.rgbHex()) }
    var entry by remember(custom.entryRgb) { mutableStateOf(custom.entryRgb.rgbHex()) }
    SectionLabel(Icons.Outlined.Palette, R.string.palette_presets)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppPalette.entries.filterNot { it == AppPalette.CUSTOM }.forEach { preset ->
            val colors = palettePreviewColors(preset)
            FilterChip(
                selected = false,
                onClick = { onSave(settings.copy(customPalette = paletteAsCustom(preset))) },
                label = { Text(stringResource(paletteLabel(preset))) },
                leadingIcon = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        colors.forEach { color -> Box(Modifier.size(7.dp).clip(CircleShape).background(color)) }
                    }
                },
            )
        }
    }
    SectionLabel(Icons.Outlined.Palette, R.string.custom_colors)
    CustomColorRow(CustomColorTarget.PRIMARY, custom.primaryRgb) { target = CustomColorTarget.PRIMARY }
    CustomColorRow(CustomColorTarget.SECONDARY, custom.secondaryRgb) { target = CustomColorTarget.SECONDARY }
    CustomColorRow(CustomColorTarget.TERTIARY, custom.tertiaryRgb) { target = CustomColorTarget.TERTIARY }
    CustomColorRow(CustomColorTarget.ENTRY, custom.entryRgb) { target = CustomColorTarget.ENTRY }
    TextButton(onClick = { showHex = !showHex }) {
        Icon(Icons.Outlined.Palette, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.exact_colors))
        Spacer(Modifier.width(4.dp))
        Icon(if (showHex) Icons.Outlined.Remove else Icons.Default.Add, contentDescription = null)
    }
    if (showHex) {
        HexColorField(primary, {
            primary = it
            parseRgbHex(it)?.let { rgb -> onSave(settings.copy(customPalette = custom.copy(primaryRgb = rgb))) }
        }, R.string.custom_primary_color)
        HexColorField(secondary, {
            secondary = it
            parseRgbHex(it)?.let { rgb -> onSave(settings.copy(customPalette = custom.copy(secondaryRgb = rgb))) }
        }, R.string.custom_secondary_color)
        HexColorField(tertiary, {
            tertiary = it
            parseRgbHex(it)?.let { rgb -> onSave(settings.copy(customPalette = custom.copy(tertiaryRgb = rgb))) }
        }, R.string.custom_tertiary_color)
        HexColorField(entry, {
            entry = it
            parseRgbHex(it)?.let { rgb -> onSave(settings.copy(customPalette = custom.copy(entryRgb = rgb))) }
        }, R.string.custom_entry_color)
    }
    OutlinedButton(
        onClick = { onSave(settings.copy(customPalette = CustomPalette())) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.reset_colors))
    }
    target?.let { selectedTarget ->
        val initial = when (selectedTarget) {
            CustomColorTarget.PRIMARY -> custom.primaryRgb
            CustomColorTarget.SECONDARY -> custom.secondaryRgb
            CustomColorTarget.TERTIARY -> custom.tertiaryRgb
            CustomColorTarget.ENTRY -> custom.entryRgb
        }
        CustomColorPickerDialog(
            target = selectedTarget,
            initialRgb = initial,
            onDismiss = { target = null },
            onSave = { rgb ->
                val updated = when (selectedTarget) {
                    CustomColorTarget.PRIMARY -> custom.copy(primaryRgb = rgb)
                    CustomColorTarget.SECONDARY -> custom.copy(secondaryRgb = rgb)
                    CustomColorTarget.TERTIARY -> custom.copy(tertiaryRgb = rgb)
                    CustomColorTarget.ENTRY -> custom.copy(entryRgb = rgb)
                }
                onSave(settings.copy(customPalette = updated))
                target = null
            },
        )
    }
}

@Composable
private fun CustomColorRow(target: CustomColorTarget, rgb: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF000000L or rgb.toLong())))
        Column(Modifier.weight(1f)) {
            Text(stringResource(target.label), fontWeight = FontWeight.SemiBold)
            Text("#${rgb.rgbHex()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun CustomColorPickerDialog(
    target: CustomColorTarget,
    initialRgb: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    val controller = rememberColorPickerController()
    var selectedRgb by remember(target, initialRgb) { mutableIntStateOf(initialRgb) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(target.label)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HsvColorPicker(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    controller = controller,
                    initialColor = Color(0xFF000000L or initialRgb.toLong()),
                    onColorChanged = { envelope -> selectedRgb = envelope.color.toArgb() and 0xFFFFFF },
                )
                BrightnessSlider(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    controller = controller,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                            .background(Color(0xFF000000L or selectedRgb.toLong())),
                    )
                    Text("#${selectedRgb.rgbHex()}", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selectedRgb) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun HexColorField(value: String, onValueChange: (String) -> Unit, @StringRes label: Int) {
    val valid = parseRgbHex(value) != null
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val cleaned = input.removePrefix("#").filter { it.digitToIntOrNull(16) != null }.take(6).uppercase()
            onValueChange(cleaned)
        },
        label = { Text(stringResource(label)) },
        prefix = { Text("#") },
        leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
        supportingText = if (valid) null else {{ Text(stringResource(R.string.hex_color_hint)) }},
        isError = !valid,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun Int.rgbHex(): String = toString(16).padStart(6, '0').uppercase()

private fun themeIcon(theme: AppTheme): ImageVector = when (theme) {
    AppTheme.SYSTEM -> Icons.Outlined.Devices
    AppTheme.LIGHT -> Icons.Outlined.WbSunny
    AppTheme.DARK -> Icons.Outlined.Bedtime
}

@StringRes
private fun themeLabel(theme: AppTheme): Int = when (theme) {
    AppTheme.SYSTEM -> R.string.theme_system
    AppTheme.LIGHT -> R.string.theme_light
    AppTheme.DARK -> R.string.theme_dark
}

@StringRes
private fun paletteLabel(palette: AppPalette): Int = when (palette) {
    AppPalette.SELIA -> R.string.palette_selia
    AppPalette.ROSE -> R.string.palette_rose
    AppPalette.OCEAN -> R.string.palette_ocean
    AppPalette.FOREST -> R.string.palette_forest
    AppPalette.SUNSET -> R.string.palette_sunset
    AppPalette.LILAC -> R.string.palette_lilac
    AppPalette.CUSTOM -> R.string.palette_custom
}

@Composable
private fun LanguageRow() {
    val current = AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()
    ChoiceRow(
        label = R.string.language,
        choices = listOf(
            ChoiceOption("", R.string.language_system),
            ChoiceOption("en", R.string.language_english),
            ChoiceOption("cs", R.string.language_czech),
            ChoiceOption("sk", R.string.language_slovak),
            ChoiceOption("de", R.string.language_german),
            ChoiceOption("pl", R.string.language_polish),
            ChoiceOption("es", R.string.language_spanish),
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
private fun SupportButton() {
    val context = LocalContext.current
    val notice = stringResource(R.string.support_notice)
    Button(
        onClick = {
            Toast.makeText(context, notice, Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(Intent.ACTION_VIEW, SUPPORT_URL.toUri()))
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        border = BorderStroke(1.dp, Color(0xFF111111)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFDD00),
            contentColor = Color(0xFF111111),
        ),
    ) {
        Icon(painterResource(R.drawable.ic_coffee), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.support_app))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DayOverviewSheet(
    day: LocalDate,
    state: AppState,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit,
    onRemovePeriod: () -> Unit,
    onSelfCare: () -> Unit,
) {
    val locale = currentLocale()
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }
    val shortDateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val log = state.logsByDay[day]
    val insight = CycleInsights.forDate(state.backup, state.forecastSnapshots, day)
    val comparison = DayOverview.compare(day, state.backup, state.forecastSnapshots)
    val canEndPeriod = log?.bleeding == true || suggestedPeriodStart(state, day) != null
    val periodEstimate = state.periodEstimates.firstOrNull { day >= it.start && day < it.endExclusive }
    val statusLabels = buildList {
        if (log?.bleeding == true) add(R.string.selected_day_recorded)
        if (periodEstimate != null) add(R.string.selected_day_estimated)
        when (insight.fertilityStatus) {
            FertilityStatus.OVULATION -> add(R.string.selected_day_ovulation)
            FertilityStatus.FERTILE -> add(R.string.selected_day_fertile)
            else -> Unit
        }
        if (isEmpty() && insight.phase == null) add(R.string.selected_day_regular)
    }
    var confirmRemovePeriod by remember(day) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (confirmRemovePeriod) {
        AlertDialog(
            onDismissRequest = { confirmRemovePeriod = false },
            title = { Text(stringResource(R.string.remove_period)) },
            text = { Text(stringResource(R.string.remove_period_body)) },
            confirmButton = {
                TextButton(onClick = onRemovePeriod) {
                    Text(stringResource(R.string.remove_period), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemovePeriod = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        sheetGesturesEnabled = false,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SheetHeader(R.string.day_overview, onDismiss)
                Text(day.format(dateFormat), color = MaterialTheme.colorScheme.onSurfaceVariant)
                SectionLabel(Icons.Outlined.WaterDrop, R.string.quick_period)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (log?.bleeding == true) {
                        Button(onClick = onEndPeriod, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.period_end_action))
                        }
                    } else {
                        Button(onClick = onStartPeriod, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Opacity, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.period_start_action))
                        }
                    }
                    if (log?.bleeding != true && canEndPeriod) {
                        OutlinedButton(onClick = onEndPeriod, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.period_end_action))
                        }
                    }
                }
                if (log?.bleeding == true) {
                    OutlinedButton(
                        onClick = { confirmRemovePeriod = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.remove_period), color = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.edit_record))
                }
                SectionLabel(Icons.Outlined.CalendarMonth, R.string.day_status)
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    statusLabels.forEach { Text(stringResource(it), fontWeight = FontWeight.SemiBold) }
                    insight.phase?.let { Text(stringResource(phaseHeadingLabel(it))) }
                    insight.fertility?.let {
                        if (day != it.ovulation) {
                            Text(stringResource(R.string.estimated_ovulation, it.ovulation.format(shortDateFormat)))
                        }
                        Text(stringResource(R.string.fertile_window_value, it.fertileStart.format(shortDateFormat), it.fertileEnd.format(shortDateFormat)))
                    }
                }
                PhaseGuidanceCard(insight, onSelfCare)
                comparison?.let {
                    SectionLabel(Icons.Outlined.EventAvailable, R.string.prediction_accuracy)
                    Text(stringResource(R.string.forecast_saved, it.snapshot.earliestStart.format(shortDateFormat), it.snapshot.latestStart.format(shortDateFormat)))
                    Text(when (it.accuracy) {
                        EstimateAccuracy.NO_REALITY -> stringResource(R.string.estimate_no_reality)
                        EstimateAccuracy.EXACT -> stringResource(R.string.estimate_exact)
                        EstimateAccuracy.EARLY -> kotlin.math.abs(requireNotNull(it.differenceDays)).let { days ->
                            pluralStringResource(R.plurals.estimate_early, days, days)
                        }
                        EstimateAccuracy.LATE -> requireNotNull(it.differenceDays).let { days ->
                            pluralStringResource(R.plurals.estimate_late, days, days)
                        }
                    })
                }
                SectionLabel(Icons.Outlined.CheckCircle, R.string.recorded_values)
                if (log == null) {
                    Text(stringResource(R.string.no_recorded_values), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (log.bleeding) Text(stringResource(R.string.flow_summary, stringResource(flowLabel(log.flow))))
                    if (log.spotting) Text(stringResource(R.string.spotting_summary))
                    log.mood?.let { Text(stringResource(R.string.mood_summary, stringResource(moodLabel(it)))) }
                    if (log.symptoms.isNotEmpty()) Text(pluralStringResource(R.plurals.symptom_count, log.symptoms.size, log.symptoms.size))
                    log.cervicalMucus?.let { Text(stringResource(R.string.cervical_summary, stringResource(cervicalMucusLabel(it)))) }
                    log.ovulationTest?.let { Text(stringResource(R.string.ovulation_test_summary, stringResource(testResultLabel(it)))) }
                    log.pregnancyTest?.let { Text(stringResource(R.string.pregnancy_test_summary, stringResource(testResultLabel(it)))) }
                    log.painLevel?.let { Text(stringResource(R.string.pain_summary, it)) }
                    log.energy?.let { Text(stringResource(R.string.energy_summary, stringResource(wellbeingLevelLabel(it)))) }
                    log.stress?.let { Text(stringResource(R.string.stress_summary, stringResource(wellbeingLevelLabel(it)))) }
                    log.activity?.let { Text(stringResource(R.string.activity_summary, stringResource(activityLevelLabel(it)))) }
                    log.medication?.let { Text(stringResource(R.string.medication_summary, stringResource(medicationStatusLabel(it)))) }
                    log.weightKg?.let { Text(stringResource(R.string.weight_summary, it)) }
                    log.temperatureC?.let { Text(stringResource(R.string.temperature_summary, it)) }
                    log.sleepHours?.let { Text(stringResource(R.string.sleep_summary, it)) }
                    log.intimacy?.let {
                        Text(stringResource(
                            R.string.intimacy_summary,
                            stringResource(if (it == Intimacy.SEX) R.string.intimacy_sex else R.string.intimacy_protected),
                        ))
                    }
                    log.note.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Spacer(Modifier.height(8.dp))
            }
            HorizontalDivider()
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

private fun suggestedPeriodStart(state: AppState, day: LocalDate): LocalDate? =
    (state.prediction.periodStarts + state.periodEstimates.map(PeriodEstimate::start))
    .filter { !it.isAfter(day) && ChronoUnit.DAYS.between(it, day) in 0..13 }
    .maxOrNull()

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DayLogSheet(
    day: LocalDate,
    initial: DayLog?,
    showFertility: Boolean,
    onDismiss: () -> Unit,
    onSave: (DayLog) -> Unit,
) {
    var flow by remember(day, initial) { mutableStateOf(initial?.flow?.takeIf { initial.bleeding } ?: Flow.NONE) }
    var spotting by remember(day, initial) { mutableStateOf(initial?.spotting == true) }
    var mood by remember(day, initial) { mutableStateOf(initial?.mood) }
    var symptoms by remember(day, initial) { mutableStateOf(initial?.symptoms.orEmpty()) }
    var note by remember(day, initial) { mutableStateOf(initial?.note.orEmpty()) }
    var showMore by rememberSaveable(day) { mutableStateOf(false) }
    var weight by remember(day, initial) { mutableStateOf(initial?.weightKg?.toString().orEmpty()) }
    var temperature by remember(day, initial) { mutableStateOf(initial?.temperatureC?.toString().orEmpty()) }
    var sleep by remember(day, initial) { mutableStateOf(initial?.sleepHours?.toString().orEmpty()) }
    var intimacy by remember(day, initial) { mutableStateOf(initial?.intimacy) }
    var cervicalMucus by remember(day, initial) { mutableStateOf(initial?.cervicalMucus) }
    var ovulationTest by remember(day, initial) { mutableStateOf(initial?.ovulationTest) }
    var pregnancyTest by remember(day, initial) { mutableStateOf(initial?.pregnancyTest) }
    var painLevel by remember(day, initial) { mutableStateOf(initial?.painLevel) }
    var energy by remember(day, initial) { mutableStateOf(initial?.energy) }
    var stress by remember(day, initial) { mutableStateOf(initial?.stress) }
    var activity by remember(day, initial) { mutableStateOf(initial?.activity) }
    var medication by remember(day, initial) { mutableStateOf(initial?.medication) }
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        sheetGesturesEnabled = false,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding(),
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SheetHeader(R.string.edit_day, onDismiss)
                Text(
                    day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChoiceRow(
                    label = R.string.flow,
                    choices = listOf(
                        ChoiceOption(Flow.NONE, R.string.flow_none, Icons.Outlined.RemoveCircleOutline),
                        ChoiceOption(Flow.UNKNOWN, R.string.flow_unknown, Icons.Outlined.WaterDrop),
                        ChoiceOption(Flow.LIGHT, R.string.flow_light, Icons.Outlined.WaterDrop),
                        ChoiceOption(Flow.MEDIUM, R.string.flow_medium, Icons.Outlined.WaterDrop),
                        ChoiceOption(Flow.HEAVY, R.string.flow_heavy, Icons.Outlined.WaterDrop),
                    ),
                    selected = flow,
                    icon = Icons.Outlined.WaterDrop,
                ) { flow = it }
                ChoiceRow(
                    label = R.string.mood,
                    choices = listOf(
                        ChoiceOption(Mood.GREAT, R.string.mood_great, Icons.Outlined.SentimentVerySatisfied),
                        ChoiceOption(Mood.GOOD, R.string.mood_good, Icons.Outlined.SentimentSatisfied),
                        ChoiceOption(Mood.OKAY, R.string.mood_okay, Icons.Outlined.SentimentNeutral),
                        ChoiceOption(Mood.LOW, R.string.mood_low, Icons.Outlined.SentimentDissatisfied),
                        ChoiceOption(Mood.BAD, R.string.mood_bad, Icons.Outlined.SentimentVeryDissatisfied),
                    ),
                    selected = mood,
                    icon = Icons.Outlined.SentimentSatisfied,
                ) { mood = if (mood == it) null else it }
                SectionLabel(Icons.Outlined.MonitorHeart, R.string.symptoms)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    symptomLabels.forEach { choice ->
                        FilterChip(
                            selected = choice.value in symptoms,
                            onClick = {
                                symptoms = if (choice.value in symptoms) {
                                    symptoms - choice.value
                                } else {
                                    symptoms + choice.value
                                }
                            },
                            label = { Text(stringResource(choice.label)) },
                            leadingIcon = choice.icon?.let { symptomIcon ->
                                { Icon(symptomIcon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            },
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
                    SwitchRow(R.string.spotting, spotting, Icons.Outlined.WaterDrop) { spotting = it }
                    if (showFertility) {
                        SectionLabel(Icons.Outlined.Spa, R.string.fertility_signs)
                        ChoiceRow(
                            label = R.string.cervical_mucus,
                            choices = cervicalMucusLabels,
                            selected = cervicalMucus,
                            icon = Icons.Outlined.WaterDrop,
                        ) { cervicalMucus = it.takeUnless { cervicalMucus == it } }
                        ChoiceRow(
                            label = R.string.ovulation_test,
                            choices = testResultLabels,
                            selected = ovulationTest,
                            icon = Icons.Outlined.WbSunny,
                        ) { ovulationTest = it.takeUnless { ovulationTest == it } }
                        ChoiceRow(
                            label = R.string.pregnancy_test,
                            choices = testResultLabels,
                            selected = pregnancyTest,
                            icon = Icons.Outlined.PregnantWoman,
                        ) { pregnancyTest = it.takeUnless { pregnancyTest == it } }
                    }
                    SectionLabel(Icons.Outlined.MonitorHeart, R.string.wellbeing_trackers)
                    PainRow(painLevel) { painLevel = it }
                    ChoiceRow(
                        label = R.string.energy,
                        choices = wellbeingLevelLabels,
                        selected = energy,
                        icon = Icons.Outlined.Bolt,
                    ) { energy = it.takeUnless { energy == it } }
                    ChoiceRow(
                        label = R.string.stress,
                        choices = wellbeingLevelLabels,
                        selected = stress,
                        icon = Icons.Outlined.Psychology,
                    ) { stress = it.takeUnless { stress == it } }
                    ChoiceRow(
                        label = R.string.activity,
                        choices = activityLevelLabels,
                        selected = activity,
                        icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    ) { activity = it.takeUnless { activity == it } }
                    ChoiceRow(
                        label = R.string.medication,
                        choices = medicationStatusLabels,
                        selected = medication,
                        icon = Icons.Outlined.Medication,
                    ) { medication = it.takeUnless { medication == it } }
                    MeasurementField(weight, { weight = it }, R.string.weight_kg, weightValid, Icons.Outlined.MonitorWeight)
                    MeasurementField(temperature, { temperature = it }, R.string.temperature_c, temperatureValid, Icons.Outlined.Thermostat)
                    MeasurementField(sleep, { sleep = it }, R.string.sleep_hours, sleepValid, Icons.Outlined.Bedtime)
                    SectionLabel(Icons.Outlined.Favorite, R.string.intimacy)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ChoiceOption(Intimacy.SEX, R.string.intimacy_sex, Icons.Outlined.Favorite),
                            ChoiceOption(Intimacy.PROTECTED, R.string.intimacy_protected, Icons.Outlined.Shield),
                        ).forEach { choice ->
                            FilterChip(
                                selected = intimacy == choice.value,
                                onClick = { intimacy = choice.value.takeUnless { intimacy == choice.value } },
                                label = { Text(stringResource(choice.label)) },
                                leadingIcon = {
                                    Icon(requireNotNull(choice.icon), contentDescription = null, modifier = Modifier.size(18.dp))
                                },
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
                        spotting = spotting,
                        flow = flow,
                        mood = mood,
                        symptoms = symptoms,
                        note = note.trim(),
                        weightKg = weightValue,
                        temperatureC = temperatureValue,
                        sleepHours = sleepValue,
                        intimacy = intimacy,
                        cervicalMucus = cervicalMucus,
                        ovulationTest = ovulationTest,
                        pregnancyTest = pregnancyTest,
                        painLevel = painLevel,
                        energy = energy,
                        stress = stress,
                        activity = activity,
                        medication = medication,
                        importedDetails = initial?.importedDetails.orEmpty(),
                    ))
                }, enabled = canSave) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@Composable
private fun PainRow(value: Int?, onChange: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(Icons.Outlined.Healing, R.string.pain_level)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..10).forEach { level ->
                FilterChip(
                    selected = value == level,
                    onClick = { onChange(level.takeUnless { value == level }) },
                    label = { Text(level.toString()) },
                )
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
    ChoiceOption(Symptom.CRAMPS, R.string.symptom_cramps, Icons.Outlined.Healing),
    ChoiceOption(Symptom.HEADACHE, R.string.symptom_headache, Icons.Outlined.PsychologyAlt),
    ChoiceOption(Symptom.BLOATING, R.string.symptom_bloating, Icons.Outlined.Air),
    ChoiceOption(Symptom.TENDER_BREASTS, R.string.symptom_tender_breasts, Icons.Outlined.FavoriteBorder),
    ChoiceOption(Symptom.FATIGUE, R.string.symptom_fatigue, Icons.Outlined.Bedtime),
    ChoiceOption(Symptom.ACNE, R.string.symptom_acne, Icons.Outlined.FaceRetouchingNatural),
    ChoiceOption(Symptom.CRAVINGS, R.string.symptom_cravings, Icons.Outlined.Restaurant),
    ChoiceOption(Symptom.BACKACHE, R.string.symptom_backache, Icons.Outlined.AirlineSeatReclineNormal),
)

private val cervicalMucusLabels = listOf(
    ChoiceOption(CervicalMucus.DRY, R.string.cervical_dry, Icons.Outlined.Air),
    ChoiceOption(CervicalMucus.STICKY, R.string.cervical_sticky, Icons.Outlined.WaterDrop),
    ChoiceOption(CervicalMucus.CREAMY, R.string.cervical_creamy, Icons.Outlined.WaterDrop),
    ChoiceOption(CervicalMucus.WATERY, R.string.cervical_watery, Icons.Outlined.WaterDrop),
    ChoiceOption(CervicalMucus.EGG_WHITE, R.string.cervical_egg_white, Icons.Outlined.WaterDrop),
    ChoiceOption(CervicalMucus.UNUSUAL, R.string.cervical_unusual, Icons.AutoMirrored.Outlined.HelpOutline),
)

private val testResultLabels = listOf(
    ChoiceOption(TestResult.NEGATIVE, R.string.test_negative, Icons.Outlined.RemoveCircleOutline),
    ChoiceOption(TestResult.POSITIVE, R.string.test_positive, Icons.Outlined.CheckCircle),
    ChoiceOption(TestResult.INVALID, R.string.test_invalid, Icons.AutoMirrored.Outlined.HelpOutline),
)

private val wellbeingLevelLabels = listOf(
    ChoiceOption(WellbeingLevel.LOW, R.string.level_low, Icons.Outlined.ArrowDownward),
    ChoiceOption(WellbeingLevel.MEDIUM, R.string.level_medium, Icons.Outlined.Remove),
    ChoiceOption(WellbeingLevel.HIGH, R.string.level_high, Icons.Outlined.ArrowUpward),
)

private val activityLevelLabels = listOf(
    ChoiceOption(ActivityLevel.LIGHT, R.string.activity_light, Icons.AutoMirrored.Outlined.DirectionsWalk),
    ChoiceOption(ActivityLevel.MODERATE, R.string.activity_moderate, Icons.AutoMirrored.Outlined.DirectionsRun),
    ChoiceOption(ActivityLevel.INTENSE, R.string.activity_intense, Icons.Outlined.FitnessCenter),
)

private val medicationStatusLabels = listOf(
    ChoiceOption(MedicationStatus.TAKEN, R.string.medication_taken, Icons.Outlined.Medication),
    ChoiceOption(MedicationStatus.MISSED, R.string.medication_missed, Icons.Outlined.EventBusy),
)

@StringRes
private fun cervicalMucusLabel(value: CervicalMucus): Int = cervicalMucusLabels.first { it.value == value }.label

@StringRes
private fun testResultLabel(value: TestResult): Int = testResultLabels.first { it.value == value }.label

@StringRes
private fun wellbeingLevelLabel(value: WellbeingLevel): Int = wellbeingLevelLabels.first { it.value == value }.label

@StringRes
private fun activityLevelLabel(value: ActivityLevel): Int = activityLevelLabels.first { it.value == value }.label

@StringRes
private fun medicationStatusLabel(value: MedicationStatus): Int = medicationStatusLabels.first { it.value == value }.label

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
