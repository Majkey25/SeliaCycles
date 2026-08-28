package com.majkeylab.seliacycles

import android.app.Application
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
    val loading: Boolean = true,
    val busy: Boolean = false,
    @param:StringRes val message: Int? = null,
) {
    val logsByDay: Map<java.time.LocalDate, DayLog> = backup.logs.associateBy(DayLog::day)

    val prediction: CyclePrediction = CyclePredictor.predict(
        bleedingDays = backup.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day),
        defaultCycleLength = backup.settings.cycleLength,
        defaultPeriodLength = backup.settings.periodLength,
    )
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = CycleStore(application)
    private val storeMutex = Mutex()
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()

    init {
        reload()
    }

    fun saveLog(log: DayLog) = runStoreAction {
        store.saveLog(log)
    }

    fun saveSettings(settings: AppSettings) {
        _state.value = _state.value.copy(backup = _state.value.backup.copy(settings = settings))
        runStoreAction {
            store.saveSettings(settings)
            ReminderWorker.sync(getApplication(), settings)
        }
    }

    fun clearAll() = runStoreAction {
        store.clearAll()
        ReminderWorker.sync(getApplication(), AppSettings())
    }

    fun permissionDenied() {
        _state.value = _state.value.copy(message = R.string.permission_denied)
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun runStoreAction(action: suspend () -> Unit) = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true, message = null)
        val result = runCatching { withContext(Dispatchers.IO) { storeMutex.withLock { action() } } }
        reload(if (result.isFailure) R.string.operation_failed else null)
    }

    private fun reload(@StringRes message: Int? = null) = viewModelScope.launch {
        val backup = runCatching {
            withContext(Dispatchers.IO) {
                storeMutex.withLock {
                    store.load().also { ReminderWorker.sync(getApplication(), it.settings) }
                }
            }
        }
        _state.value = backup.fold(
            onSuccess = { AppState(backup = it, loading = false, message = message) },
            onFailure = { _state.value.copy(loading = false, busy = false, message = R.string.operation_failed) },
        )
    }

    override fun onCleared() {
        store.close()
    }
}
