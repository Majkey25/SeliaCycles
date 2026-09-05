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

data class CycleContent(
    val backup: CycleBackup = CycleBackup(),
    val forecastSnapshots: Map<java.time.YearMonth, ForecastSnapshot> = emptyMap(),
    val referenceDate: java.time.LocalDate = java.time.LocalDate.now(),
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

data class AppState(
    val content: CycleContent = CycleContent(),
    val calendarPermissionGranted: Boolean = false,
    val selectedCalendarId: Long? = null,
    val deviceCalendars: List<DeviceCalendar> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val myCalendarPreview: MyCalendarPreview? = null,
    @param:StringRes val message: Int? = null,
) {
    val backup get() = content.backup
    val forecastSnapshots get() = content.forecastSnapshots
    val referenceDate get() = content.referenceDate
    val logsByDay get() = content.logsByDay
    val prediction get() = content.prediction
    val periodEstimates get() = content.periodEstimates
    val todayInsight get() = content.todayInsight
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = CycleStore(application)
    private val calendarMirror = CalendarMirror(application)
    private val myCalendarImporter = MyCalendarImporter(application)
    private val myCalendarExporter = MyCalendarExporter(application)
    private val storeMutex = Mutex()
    private var storeRevision = 0L
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
        _state.value = _state.value.copy(content = _state.value.content.copy(backup = _state.value.backup.copy(settings = settings)))
        runStoreAction {
            store.saveSettings(settings)
        }
    }

    fun clearAll() = viewModelScope.launch {
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null, myCalendarPreview = null)
        val result = runCatching {
            storeMutex.withLock {
                withContext(Dispatchers.IO) {
                    val calendarConnected = calendarMirror.selectedCalendarId() != null
                    store.clearAll()
                    ReminderWorker.sync(getApplication(), AppSettings())
                    calendarConnected && runCatching { calendarMirror.disconnect() }.isFailure
                }
            }
        }
        if (revision != storeRevision) return@launch
        reload(result.fold(
            onSuccess = { calendarFailed -> if (calendarFailed) R.string.calendar_cleanup_pending else null },
            onFailure = { R.string.operation_failed },
        ), revision)
    }

    fun calendarPermissionChanged() {
        if (!_state.value.busy) reload()
    }

    fun refreshForToday() {
        if (!_state.value.busy && _state.value.referenceDate != java.time.LocalDate.now()) reload()
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
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null, myCalendarPreview = null)
        val result = runCatching {
            storeMutex.withLock {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use(myCalendarImporter::inspect)
                        ?: error("Cannot open My Calendar backup")
                }
            }
        }
        if (revision != storeRevision) return@launch
        result.fold(
            onSuccess = { preview ->
                _state.value = _state.value.copy(myCalendarPreview = preview)
                reload(revision = revision)
            },
            onFailure = { error ->
                Log.e("SeliaCycles", "My Calendar import failed", error)
                reload(
                    message = when ((error as? MyCalendarFormatException)?.failure) {
                        MyCalendarFailure.UNSUPPORTED -> R.string.my_calendar_import_unsupported
                        MyCalendarFailure.EMPTY -> R.string.my_calendar_import_empty
                        MyCalendarFailure.DAMAGED, null -> R.string.my_calendar_import_damaged
                    },
                    revision = revision,
                )
            },
        )
    }

    fun confirmMyCalendarImport() {
        val preview = _state.value.myCalendarPreview ?: return
        _state.value = _state.value.copy(myCalendarPreview = null)
        runStoreAction(R.string.my_calendar_import_complete) {
            preview.seliaTransfer?.let(store::mergeTransfer) ?: store.mergeImported(preview.logs)
        }
    }

    fun exportMyCalendar(uri: Uri) = viewModelScope.launch {
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null)
        val result = runCatching {
            storeMutex.withLock {
                withContext(Dispatchers.IO) {
                    val transfer = SeliaTransfer(store.load(), store.loadForecastSnapshots())
                    require(transfer.backup.logs.isNotEmpty())
                    getApplication<Application>().contentResolver.openOutputStream(uri, "w")?.use {
                        myCalendarExporter.write(transfer, it)
                    } ?: error("Cannot create backup")
                }
            }
        }
        result.exceptionOrNull()?.let { Log.e("SeliaCycles", "My Calendar export failed", it) }
        if (revision == storeRevision) {
            reload(if (result.isSuccess) R.string.my_calendar_export_complete else R.string.operation_failed, revision)
        }
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
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null)
        val result = runCatching { storeMutex.withLock { withContext(Dispatchers.IO) { action() } } }
        if (revision == storeRevision) reload(if (result.isFailure) R.string.operation_failed else successMessage, revision)
    }

    private fun reload(@StringRes message: Int? = null, revision: Long = storeRevision) = viewModelScope.launch {
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
                        content = CycleContent(backup, forecastSnapshots, referenceDate),
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
        if (revision != storeRevision) return@launch
        _state.value = loaded.fold(
            onSuccess = {
                AppState(
                    content = it.content,
                    calendarPermissionGranted = it.calendar.permissionGranted,
                    selectedCalendarId = it.calendar.selectedCalendarId,
                    deviceCalendars = it.calendar.calendars,
                    loading = false,
                    myCalendarPreview = _state.value.myCalendarPreview,
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
    val content: CycleContent,
    val calendar: CalendarMirrorSnapshot,
    val calendarFailed: Boolean,
)
