package com.majkeylab.seliacycles

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AppState(
    val backup: CycleBackup = CycleBackup(),
    val forecastSnapshots: Map<java.time.YearMonth, ForecastSnapshot> = emptyMap(),
    val referenceDate: java.time.LocalDate = java.time.LocalDate.now(),
    val calendarPermissionGranted: Boolean = false,
    val selectedCalendarId: Long? = null,
    val deviceCalendars: List<DeviceCalendar> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val myCalendarPreview: MyCalendarPreview? = null,
    @param:StringRes val message: Int? = null,
) {
    val logsByDay: Map<java.time.LocalDate, DayLog> = backup.logs.associateBy(DayLog::day)

    val prediction: CyclePrediction = CyclePredictor.predict(
        bleedingDays = backup.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day),
        defaultCycleLength = backup.settings.cycleLength,
        defaultPeriodLength = backup.settings.periodLength,
        cycleLengthOverride = backup.settings.cycleLengthOverride,
        periodLengthOverride = backup.settings.periodLengthOverride,
        activePeriodStart = backup.settings.activePeriodStart,
        referenceDate = referenceDate,
    )

    val periodEstimates: List<PeriodEstimate> = CycleInsights.calendarPeriodEstimates(
        backup,
        forecastSnapshots,
        referenceDate,
    )

    val todayInsight: DailyCycleInsight = CycleInsights.forDate(backup, forecastSnapshots, referenceDate)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = CycleStore(application)
    private val calendarMirror = CalendarMirror(application)
    private val myCalendarImporter = MyCalendarImporter(application)
    private val myCalendarExporter = MyCalendarExporter(application)
    private val storeMutex = Mutex()
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()

    init {
        reload()
    }

    fun saveLog(log: DayLog) = runStoreAction {
        store.saveLog(log)
    }

    fun startPeriod(day: java.time.LocalDate) = runStoreAction {
        val backup = store.load()
        store.savePeriodState(
            PeriodActions.start(day, backup.logs),
            backup.settings.copy(activePeriodStart = day),
        )
    }

    fun endPeriod(day: java.time.LocalDate, suggestedStart: java.time.LocalDate?) = runStoreAction {
        val backup = store.load()
        val start = backup.settings.activePeriodStart ?: suggestedStart
        store.savePeriodState(
            PeriodActions.end(day, backup.logs, start),
            backup.settings.copy(activePeriodStart = null),
        )
    }

    fun savePeriodDays(day: java.time.LocalDate, selectedDays: Set<java.time.LocalDate>) = runStoreAction {
        val backup = store.load()
        val today = java.time.LocalDate.now()
        val originalDays = PeriodActions.periodDays(day, backup.logs)
        val logs = PeriodActions.replace(day, selectedDays, backup.logs, today)
        val active = when (backup.settings.activePeriodStart) {
            in originalDays -> selectedDays.minOrNull()
            null -> selectedDays.minOrNull()?.takeIf { selectedDays.maxOrNull() == today }
            else -> backup.settings.activePeriodStart
        }
        store.savePeriodState(logs, backup.settings.copy(activePeriodStart = active))
    }

    fun saveSettings(settings: AppSettings) {
        _state.value = _state.value.copy(backup = _state.value.backup.copy(settings = settings))
        runStoreAction {
            store.saveSettings(settings)
        }
    }

    fun clearAll() = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true, message = null)
        val result = runCatching {
            withContext(Dispatchers.IO) {
                storeMutex.withLock {
                    val calendarConnected = calendarMirror.selectedCalendarId() != null
                    store.clearAll()
                    ReminderWorker.sync(getApplication(), AppSettings())
                    calendarConnected && runCatching { calendarMirror.disconnect() }.isFailure
                }
            }
        }
        reload(result.fold(
            onSuccess = { calendarFailed -> if (calendarFailed) R.string.calendar_cleanup_pending else null },
            onFailure = { R.string.operation_failed },
        ))
    }

    fun calendarPermissionChanged() = reload()

    fun refreshForToday() {
        if (_state.value.referenceDate != java.time.LocalDate.now()) reload()
    }

    fun connectCalendar(calendarId: Long) = runStoreAction {
        calendarMirror.connect(
            calendarId = calendarId,
            backup = store.load(),
            snapshots = store.loadForecastSnapshots().associateBy(ForecastSnapshot::month),
        )
    }

    fun disconnectCalendar() = runStoreAction {
        val settings = store.load().settings
        if (settings.partnerViewEnabled) store.saveSettings(settings.copy(partnerViewEnabled = false))
        calendarMirror.disconnect()
    }

    fun inspectMyCalendar(uri: Uri) = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true, message = null, myCalendarPreview = null)
        val result = runCatching {
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use(myCalendarImporter::inspect)
                    ?: error("Cannot open My Calendar backup")
            }
        }
        result.fold(
            onSuccess = { preview -> _state.value = _state.value.copy(busy = false, myCalendarPreview = preview) },
            onFailure = { error ->
                Log.e("SeliaCycles", "My Calendar import failed", error)
                _state.value = _state.value.copy(
                    busy = false,
                    message = when ((error as? MyCalendarFormatException)?.failure) {
                        MyCalendarFailure.UNSUPPORTED -> R.string.my_calendar_import_unsupported
                        MyCalendarFailure.EMPTY -> R.string.my_calendar_import_empty
                        MyCalendarFailure.DAMAGED, null -> R.string.my_calendar_import_damaged
                    },
                )
            },
        )
    }

    fun confirmMyCalendarImport() {
        val preview = _state.value.myCalendarPreview ?: return
        runStoreAction(R.string.my_calendar_import_complete) {
            preview.seliaTransfer?.let(store::mergeTransfer) ?: store.mergeImported(preview.logs)
        }
    }

    fun exportMyCalendar(uri: Uri) = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true, message = null)
        val result = runCatching {
            withContext(Dispatchers.IO) {
                storeMutex.withLock {
                    val transfer = SeliaTransfer(store.load(), store.loadForecastSnapshots())
                    require(transfer.backup.logs.isNotEmpty())
                    getApplication<Application>().contentResolver.openOutputStream(uri, "w")?.use {
                        myCalendarExporter.write(transfer, it)
                    } ?: error("Cannot create backup")
                }
            }
        }
        result.exceptionOrNull()?.let { Log.e("SeliaCycles", "My Calendar export failed", it) }
        reload(if (result.isSuccess) R.string.my_calendar_export_complete else R.string.operation_failed)
    }

    fun cancelMyCalendarImport() {
        _state.value = _state.value.copy(myCalendarPreview = null)
    }

    fun permissionDenied() {
        _state.value = _state.value.copy(message = R.string.permission_denied)
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun runStoreAction(@StringRes successMessage: Int? = null, action: suspend () -> Unit) = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true, message = null)
        val result = runCatching { withContext(Dispatchers.IO) { storeMutex.withLock { action() } } }
        reload(if (result.isFailure) R.string.operation_failed else successMessage)
    }

    private fun reload(@StringRes message: Int? = null) = viewModelScope.launch {
        val referenceDate = java.time.LocalDate.now()
        val loaded = runCatching {
            withContext(Dispatchers.IO) {
                storeMutex.withLock {
                    val backup = store.load().also { ReminderWorker.sync(getApplication(), it.settings) }
                    val existingSnapshots = store.loadForecastSnapshots()
                    val missingSnapshots = ForecastSnapshotPlanner.missingSnapshots(
                        backup = backup,
                        existing = existingSnapshots.associateBy(ForecastSnapshot::month),
                        referenceDate = referenceDate,
                    )
                    store.saveForecastSnapshots(missingSnapshots)
                    val forecastSnapshots = (existingSnapshots + missingSnapshots).associateBy(ForecastSnapshot::month)
                    val calendar = runCatching { calendarMirror.snapshot(backup, forecastSnapshots) }
                    LoadedState(
                        backup = backup,
                        forecastSnapshots = forecastSnapshots,
                        calendar = calendar.getOrElse {
                            CalendarMirrorSnapshot(
                                permissionGranted = calendarMirror.hasPermissions(),
                                selectedCalendarId = calendarMirror.selectedCalendarId(),
                                calendars = emptyList(),
                            )
                        },
                        calendarFailed = calendar.isFailure,
                    )
                }
            }
        }
        _state.value = loaded.fold(
            onSuccess = {
                AppState(
                    backup = it.backup,
                    forecastSnapshots = it.forecastSnapshots,
                    referenceDate = referenceDate,
                    calendarPermissionGranted = it.calendar.permissionGranted,
                    selectedCalendarId = it.calendar.selectedCalendarId,
                    deviceCalendars = it.calendar.calendars,
                    loading = false,
                    message = message ?: if (it.calendarFailed) R.string.calendar_sync_failed else null,
                )
            },
            onFailure = { _state.value.copy(loading = false, busy = false, message = R.string.operation_failed) },
        )
    }

    override fun onCleared() {
        store.close()
    }
}

private data class LoadedState(
    val backup: CycleBackup,
    val forecastSnapshots: Map<java.time.YearMonth, ForecastSnapshot>,
    val calendar: CalendarMirrorSnapshot,
    val calendarFailed: Boolean,
)
