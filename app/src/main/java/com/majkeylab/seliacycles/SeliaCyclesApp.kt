package com.majkeylab.seliacycles

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
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

@Composable
fun SeliaCyclesApp(state: AppState, viewModel: MainViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.TODAY) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var infoDialog by remember { mutableStateOf<InfoDialog?>(null) }
    var showExportPassword by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var exportPassword by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val password = exportPassword
        exportPassword = null
        if (uri != null && password != null) viewModel.exportBackup(uri, password)
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importUri = uri
    }
    val healthPermission = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.containsAll(HealthConnectImporter.permissions)) viewModel.importHealthConnect()
        else viewModel.permissionDenied()
    }
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
                    Screen.TODAY -> TodayScreen(state, onEdit = { selectedDay = LocalDate.now() })
                    Screen.CALENDAR -> CalendarScreen(state, onEdit = { selectedDay = it })
                    Screen.HISTORY -> HistoryScreen(state)
                    Screen.SETTINGS -> SettingsScreen(
                        state = state,
                        onSave = viewModel::saveSettings,
                        onCreateBackup = { showExportPassword = true },
                        onRestoreBackup = { openBackup.launch(arrayOf("application/octet-stream", "*/*")) },
                        onHealthImport = {
                            if (viewModel.healthConnectStatus == HealthConnectClient.SDK_AVAILABLE) {
                                healthPermission.launch(HealthConnectImporter.permissions)
                            } else {
                                viewModel.healthConnectUnavailableMessage()
                            }
                        },
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
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
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
    if (showExportPassword) {
        PasswordDialog(
            title = R.string.backup_password_title,
            onDismiss = { showExportPassword = false },
            onConfirm = { password ->
                showExportPassword = false
                exportPassword = password
                createBackup.launch("SeliaCycles-${LocalDate.now()}.seliabackup")
            },
        )
    }
    importUri?.let { uri ->
        PasswordDialog(
            title = R.string.restore_password_title,
            extraMessage = R.string.restore_warning,
            onDismiss = { importUri = null },
            onConfirm = { password ->
                importUri = null
                viewModel.restoreBackup(uri, password)
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
private fun TodayScreen(state: AppState, onEdit: () -> Unit) {
    val today = LocalDate.now()
    val prediction = state.prediction
    val next = prediction.nextPeriodStart.takeIf { state.backup.settings.predictionsEnabled }
    val distance = next?.let { ChronoUnit.DAYS.between(today, it).toInt() }
    val locale = currentLocale()
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(stringResource(R.string.today_heading), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.next_period), style = MaterialTheme.typography.labelLarge)
            Text(
                next?.format(dateFormat) ?: stringResource(R.string.no_period_data),
                style = if (next == null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = if (next == null) FontWeight.Normal else FontWeight.SemiBold,
            )
            when {
                distance == null -> Unit
                distance > 0 -> Text(pluralStringResource(R.plurals.days_until_period, distance, distance))
                distance == 0 -> Text(stringResource(R.string.predicted_today))
                else -> Text(pluralStringResource(R.plurals.period_late, -distance, -distance))
            }
        }
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (state.logsByDay.containsKey(today)) stringResource(R.string.today_logged) else stringResource(R.string.log_today))
        }
        if (next != null) Text(
            stringResource(R.string.estimate_notice),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CalendarScreen(state: AppState, onEdit: (LocalDate) -> Unit) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val shownMonth = YearMonth.parse(month)
    val locale = currentLocale()
    val firstDay = state.backup.settings.firstDayOfWeek
    val weekdays = if (firstDay == DayOfWeek.MONDAY) DayOfWeek.entries else listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)
    val leading = (shownMonth.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val cells = leading + shownMonth.lengthOfMonth()
    val rows = (cells + 6) / 7
    val predicted = state.prediction.nextPeriodStart
        ?.takeIf { state.backup.settings.predictionsEnabled }
        ?.let { start -> (0 until state.prediction.averagePeriodLength).map { start.plusDays(it.toLong()) }.toSet() }
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
                Text("‹", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { contentDescription = previousMonthLabel })
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
                Text("›", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { contentDescription = nextMonthLabel })
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
    modifier: Modifier,
) {
    val background = when {
        recorded -> MaterialTheme.colorScheme.primary
        predicted -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val foreground = if (recorded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val date = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(currentLocale()))
    Box(
        modifier = modifier.padding(3.dp).clip(CircleShape).background(background)
            .then(if (day == LocalDate.now()) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick).semantics { contentDescription = date },
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
            Metric(R.string.average_cycle, pluralStringResource(R.plurals.days_value, prediction.averageCycleLength, prediction.averageCycleLength), Modifier.weight(1f))
            Metric(R.string.average_period, pluralStringResource(R.plurals.days_value, prediction.averagePeriodLength, prediction.averagePeriodLength), Modifier.weight(1f))
            Metric(R.string.recorded_cycles, prediction.periodStarts.size.toString(), Modifier.weight(1f))
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
private fun Metric(@StringRes label: Int, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SettingsScreen(
    state: AppState,
    onSave: (AppSettings) -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onHealthImport: () -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onInfo: (InfoDialog) -> Unit,
    onDeleteAll: () -> Unit,
) {
    val settings = state.backup.settings
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_heading), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        SectionTitle(R.string.section_tracking)
        SwitchRow(R.string.predictions, settings.predictionsEnabled) { onSave(settings.copy(predictionsEnabled = it)) }
        Stepper(R.string.default_cycle_length, settings.cycleLength, 15..90) { onSave(settings.copy(cycleLength = it)) }
        Stepper(R.string.default_period_length, settings.periodLength, 1..14) { onSave(settings.copy(periodLength = it)) }
        ChoiceRow(
            label = R.string.first_day_of_week,
            choices = listOf(DayOfWeek.MONDAY to R.string.monday, DayOfWeek.SUNDAY to R.string.sunday),
            selected = settings.firstDayOfWeek,
        ) { onSave(settings.copy(firstDayOfWeek = it)) }

        SectionTitle(R.string.section_appearance)
        ChoiceRow(
            label = R.string.theme,
            choices = listOf(AppTheme.SYSTEM to R.string.theme_system, AppTheme.LIGHT to R.string.theme_light, AppTheme.DARK to R.string.theme_dark),
            selected = settings.theme,
        ) { onSave(settings.copy(theme = it)) }
        LanguageRow()

        SectionTitle(R.string.section_reminders)
        SwitchRow(R.string.period_reminder, settings.reminderEnabled, onReminderChange)
        if (settings.reminderEnabled) {
            Stepper(R.string.remind_before, settings.reminderDays, 0..14) { onSave(settings.copy(reminderDays = it)) }
        }

        SectionTitle(R.string.section_data)
        InfoBlock(R.string.cloud_backup, R.string.cloud_backup_body)
        Button(onClick = onCreateBackup, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Text(stringResource(R.string.create_backup)) }
        OutlinedButton(onClick = onRestoreBackup, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Text(stringResource(R.string.restore_backup)) }
        InfoBlock(R.string.health_connect, R.string.health_connect_body)
        OutlinedButton(onClick = onHealthImport, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Text(stringResource(R.string.import_data)) }
        InfoBlock(R.string.other_apps, R.string.other_apps_body)
        OutlinedButton(onClick = onDeleteAll, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
            Text(stringResource(R.string.delete_all_data), color = MaterialTheme.colorScheme.error)
        }

        SectionTitle(R.string.section_about)
        SettingsLink(R.string.privacy) { onInfo(InfoDialog.PRIVACY) }
        SettingsLink(R.string.about_cycle) { onInfo(InfoDialog.CYCLE) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
    Text(
        stringResource(text),
        modifier = Modifier.padding(top = 16.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SwitchRow(@StringRes label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Stepper(@StringRes label: Int, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val labelText = stringResource(label)
    val decrease = stringResource(R.string.decrease_value, labelText)
    val increase = stringResource(R.string.increase_value, labelText)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(labelText, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) {
            Text("−", modifier = Modifier.semantics { contentDescription = decrease })
        }
        Text(value.toString(), modifier = Modifier.width(36.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) {
            Text("+", modifier = Modifier.semantics { contentDescription = increase })
        }
    }
}

@Composable
private fun <T> ChoiceRow(@StringRes label: Int, choices: List<Pair<T, Int>>, selected: T?, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label))
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
        choices = listOf("" to R.string.language_system, "en" to R.string.language_english, "cs" to R.string.language_czech),
        selected = current,
    ) { language -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language)) }
}

@Composable
private fun InfoBlock(@StringRes title: Int, @StringRes body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsLink(@StringRes label: Int, onClick: () -> Unit) {
    Text(
        stringResource(label),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 14.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DayLogSheet(day: LocalDate, initial: DayLog?, onDismiss: () -> Unit, onSave: (DayLog) -> Unit) {
    var bleeding by remember(day, initial) { mutableStateOf(initial?.bleeding == true) }
    var flow by remember(day, initial) { mutableStateOf(initial?.flow ?: Flow.UNKNOWN) }
    var mood by remember(day, initial) { mutableStateOf(initial?.mood) }
    var symptoms by remember(day, initial) { mutableStateOf(initial?.symptoms.orEmpty()) }
    var note by remember(day, initial) { mutableStateOf(initial?.note.orEmpty()) }
    val locale = currentLocale()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.edit_day), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)), color = MaterialTheme.colorScheme.onSurfaceVariant)
            SwitchRow(R.string.bleeding, bleeding) { bleeding = it; if (!it) flow = Flow.NONE else if (flow == Flow.NONE) flow = Flow.UNKNOWN }
            if (bleeding) {
                ChoiceRow(
                    label = R.string.flow,
                    choices = listOf(
                        Flow.UNKNOWN to R.string.flow_unknown,
                        Flow.LIGHT to R.string.flow_light,
                        Flow.MEDIUM to R.string.flow_medium,
                        Flow.HEAVY to R.string.flow_heavy,
                    ),
                    selected = flow,
                ) { flow = it }
            }
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
            ) { mood = if (mood == it) null else it }
            Text(stringResource(R.string.symptoms))
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
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (initial != null) {
                    TextButton(onClick = { onSave(DayLog(day)) }) { Text(stringResource(R.string.delete_record)) }
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = {
                    onSave(DayLog(
                        day = day,
                        bleeding = bleeding,
                        flow = if (bleeding) flow else Flow.NONE,
                        mood = mood,
                        symptoms = symptoms,
                        note = note.trim(),
                    ))
                }) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

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
private fun PasswordDialog(
    @StringRes title: Int,
    @StringRes extraMessage: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(128) },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Text(stringResource(R.string.password_help), style = MaterialTheme.typography.bodySmall)
                extraMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = password.length >= 8) { Text(stringResource(R.string.continue_action)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

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
