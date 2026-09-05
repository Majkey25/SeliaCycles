package com.majkeylab.seliacycles

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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
    val activeProfile: LocalProfile = LocalProfile(LocalProfiles.DEFAULT_ID, ""),
    val profiles: List<LocalProfile> = listOf(activeProfile),
    val calendarPermissionGranted: Boolean = false,
    val selectedCalendarId: Long? = null,
    val deviceCalendars: List<DeviceCalendar> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
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
    private val localProfiles = LocalProfiles(application)
    private var session = openSession(localProfiles.selected())
    private val myCalendarImporter = MyCalendarImporter(application)
    private val myCalendarExporter = MyCalendarExporter(application)
    private val storeMutex = Mutex()
    private var storeRevision = 0L
    private val _state = MutableStateFlow(AppState(activeProfile = session.profile, profiles = localProfiles.profiles()))
    val state = _state.asStateFlow()

    init {
        reload()
        viewModelScope.launch {
            runCatching { storeMutex.withLock {
                withContext(Dispatchers.IO) {
                    localProfiles.profiles().forEach { profile ->
                        CycleStore(application, profile.id).use { ReminderWorker.sync(application, it.load().settings, profile.id) }
                    }
                }
            } }.onFailure {
                Log.e("SeliaCycles", "Profile reminder initialization failed", it)
                _state.value = _state.value.copy(message = R.string.operation_failed)
            }
        }
    }

    private fun openSession(profile: LocalProfile): ProfileSession = ProfileSession(
        profile, CycleStore(getApplication(), profile.id), CalendarMirror(getApplication(), profile.id),
    )

    fun selectProfile(id: String) {
        val profile = _state.value.profiles.firstOrNull { it.id == id } ?: return
        if (_state.value.activeProfile.id == id && !_state.value.loading) return
        val revision = ++storeRevision
        _state.value = AppState(activeProfile = profile, profiles = _state.value.profiles, busy = true)
        viewModelScope.launch {
            val result = runCatching {
                storeMutex.withLock {
                    if (revision != storeRevision) return@withLock
                    withContext(NonCancellable) {
                        val next = withContext(Dispatchers.IO) {
                            val currentProfile = localProfiles.profiles().first { it.id == id }
                            val opened = openSession(currentProfile)
                            try {
                                localProfiles.select(id)
                                session.store.close()
                            } catch (error: Exception) {
                                opened.store.close()
                                throw error
                            }
                            opened
                        }
                        session = next
                    }
                }
            }
            if (revision == storeRevision) reload(if (result.isFailure) R.string.operation_failed else null, revision)
        }
    }

    fun createProfile(name: String, mode: UiMode) = viewModelScope.launch {
        if (!acceptsProfile()) return@launch
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null)
        val result = runCatching {
            storeMutex.withLock { withContext(Dispatchers.IO) {
                localProfiles.create(name, mode) to localProfiles.profiles()
            } }
        }
        if (revision != storeRevision) return@launch
        result.fold(onSuccess = { (profile, profiles) ->
            _state.value = _state.value.copy(profiles = profiles, busy = false)
            selectProfile(profile.id)
        }, onFailure = { reload(R.string.operation_failed, revision) })
    }

    fun updateProfile(name: String, mode: UiMode) = runStoreAction {
        localProfiles.update(profile.id, name, mode)
    }

    fun deleteProfile() = viewModelScope.launch {
        if (!acceptsProfile()) return@launch
        val target = session
        if (target.profile.id == LocalProfiles.DEFAULT_ID) return@launch
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null, myCalendarPreview = null)
        val result = runCatching {
            storeMutex.withLock {
                check(session === target)
                withContext(NonCancellable) {
                    val next = withContext(Dispatchers.IO) {
                        if (target.calendarMirror.selectedCalendarId() != null) target.calendarMirror.disconnect()
                        ReminderWorker.cancel(getApplication(), target.profile.id)
                        target.store.close()
                        check(getApplication<Application>().deleteDatabase(profileDatabaseName(target.profile.id)))
                        localProfiles.remove(target.profile.id)
                        openSession(localProfiles.selected())
                    }
                    session = next
                }
            }
        }
        if (revision == storeRevision) reload(if (result.isFailure) R.string.operation_failed else null, revision)
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

    fun saveSettings(settings: AppSettings, expectedProfileId: String? = _state.value.activeProfile.id) {
        if (!acceptsProfile(expectedProfileId)) return
        _state.value = _state.value.copy(content = _state.value.content.copy(backup = _state.value.backup.copy(settings = settings)))
        runStoreAction {
            store.saveSettings(settings)
        }
    }

    fun clearAll() = viewModelScope.launch {
        if (!acceptsProfile()) return@launch
        val target = session
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null, myCalendarPreview = null)
        val result = runCatching {
            storeMutex.withLock {
                check(session === target)
                withContext(Dispatchers.IO) {
                    val calendarConnected = target.calendarMirror.selectedCalendarId() != null
                    target.store.clearAll()
                    ReminderWorker.cancel(getApplication(), target.profile.id)
                    calendarConnected && runCatching { target.calendarMirror.disconnect() }.isFailure
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
        if (!_state.value.busy && !_state.value.loadFailed) reload()
    }

    fun refreshForToday() {
        if (!_state.value.busy && !_state.value.loadFailed && _state.value.referenceDate != java.time.LocalDate.now()) reload()
    }

    fun retryLoad() {
        if (!_state.value.loadFailed || _state.value.loading || _state.value.busy) return
        val revision = ++storeRevision
        _state.value = _state.value.copy(loading = true, loadFailed = false, busy = true, message = null)
        reload(revision = revision)
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

    fun inspectMyCalendar(uri: Uri, expectedProfileId: String? = _state.value.activeProfile.id) = viewModelScope.launch {
        if (!acceptsProfile(expectedProfileId)) return@launch
        val target = session
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null, myCalendarPreview = null)
        val result = runCatching {
            storeMutex.withLock {
                check(session === target)
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

    fun exportMyCalendar(uri: Uri, expectedProfileId: String? = _state.value.activeProfile.id) = viewModelScope.launch {
        if (!acceptsProfile(expectedProfileId)) return@launch
        val target = session
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null)
        val result = runCatching {
            storeMutex.withLock {
                check(session === target)
                withContext(Dispatchers.IO) {
                    val transfer = SeliaTransfer(target.store.load(), target.store.loadForecastSnapshots())
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

    private fun runStoreAction(@StringRes successMessage: Int? = null, action: suspend ProfileSession.() -> Unit) = viewModelScope.launch {
        if (!acceptsProfile()) return@launch
        val target = session
        val revision = ++storeRevision
        _state.value = _state.value.copy(busy = true, message = null)
        val result = runCatching { storeMutex.withLock {
            check(session === target)
            withContext(Dispatchers.IO) { action(target) }
        } }
        if (revision == storeRevision) reload(if (result.isFailure) R.string.operation_failed else successMessage, revision)
    }

    private fun reload(@StringRes message: Int? = null, revision: Long = storeRevision) = viewModelScope.launch {
        val referenceDate = java.time.LocalDate.now()
        val loaded = runCatching {
            storeMutex.withLock {
                if (revision != storeRevision) return@launch
                val target = session
                withContext(Dispatchers.IO) {
                    val backup = target.store.load().also { ReminderWorker.sync(getApplication(), it.settings, target.profile.id) }
                    val existingSnapshots = target.store.loadForecastSnapshots()
                    val missingSnapshots = ForecastSnapshotPlanner.missingSnapshots(
                        backup = backup,
                        existing = existingSnapshots.associateBy(ForecastSnapshot::month),
                        referenceDate = referenceDate,
                    )
                    target.store.saveForecastSnapshots(missingSnapshots)
                    val forecastSnapshots = (existingSnapshots + missingSnapshots).associateBy(ForecastSnapshot::month)
                    val calendar = runCatching { target.calendarMirror.snapshot(backup, forecastSnapshots) }
                    LoadedState(
                        content = CycleContent(backup, forecastSnapshots, referenceDate),
                        profiles = localProfiles.profiles(),
                        calendar = calendar.getOrElse {
                            CalendarMirrorSnapshot(
                                permissionGranted = target.calendarMirror.hasPermissions(),
                                selectedCalendarId = target.calendarMirror.selectedCalendarId(),
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
                    profiles = it.profiles,
                    activeProfile = it.profiles.first { profile -> profile.id == session.profile.id },
                    calendarPermissionGranted = it.calendar.permissionGranted,
                    selectedCalendarId = it.calendar.selectedCalendarId,
                    deviceCalendars = it.calendar.calendars,
                    loading = false,
                    myCalendarPreview = _state.value.myCalendarPreview,
                    message = message ?: if (it.calendarFailed) R.string.calendar_sync_failed else null,
                )
            },
            onFailure = {
                Log.e("SeliaCycles", "Profile reload failed", it)
                _state.value.copy(
                    activeProfile = session.profile,
                    content = if (_state.value.activeProfile.id == session.profile.id) _state.value.content else CycleContent(),
                    loading = false, loadFailed = true, busy = false, myCalendarPreview = null,
                    message = R.string.operation_failed,
                )
            },
        )
    }

    private fun acceptsProfile(expectedProfileId: String? = _state.value.activeProfile.id): Boolean {
        val current = _state.value
        val accepted = !current.loading && !current.loadFailed && current.activeProfile.id == session.profile.id &&
            expectedProfileId == session.profile.id
        if (!accepted) _state.value = current.copy(message = R.string.operation_failed)
        return accepted
    }

    override fun onCleared() {
        CoroutineScope(Dispatchers.IO).launch { storeMutex.withLock { session.store.close() } }
    }
}

private data class LoadedState(
    val content: CycleContent,
    val profiles: List<LocalProfile>,
    val calendar: CalendarMirrorSnapshot,
    val calendarFailed: Boolean,
)

private data class ProfileSession(val profile: LocalProfile, val store: CycleStore, val calendarMirror: CalendarMirror)
