package com.majkeylab.seliacycles

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.ByteArrayOutputStream
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
    private val healthConnect = HealthConnectImporter(application)
    private val storeMutex = Mutex()
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()

    val healthConnectStatus: Int
        get() = healthConnect.status

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

    fun exportBackup(uri: Uri, password: String) = viewModelScope.launch {
        setBusy(true)
        val passwordChars = password.toCharArray()
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val encrypted = storeMutex.withLock { BackupCodec.encrypt(store.load(), passwordChars) }
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(encrypted)
                } ?: error("Cannot open backup destination")
            }
        }
        passwordChars.fill('\u0000')
        setResult(if (result.isSuccess) R.string.backup_saved else R.string.operation_failed)
    }

    fun restoreBackup(uri: Uri, password: String) = viewModelScope.launch {
        setBusy(true)
        val passwordChars = password.toCharArray()
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val encrypted = getApplication<Application>().contentResolver.openInputStream(uri)?.use(::readBounded)
                    ?: error("Cannot open backup")
                val backup = BackupCodec.decrypt(encrypted, passwordChars)
                storeMutex.withLock {
                    store.replace(backup)
                    ReminderWorker.sync(getApplication(), backup.settings)
                }
            }
        }
        passwordChars.fill('\u0000')
        if (result.isSuccess) {
            reload(R.string.backup_restored)
        } else {
            setResult(R.string.backup_restore_failed)
        }
    }

    fun importHealthConnect() = viewModelScope.launch {
        setBusy(true)
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val imported = healthConnect.importLogs()
                storeMutex.withLock { store.mergeImported(imported) }
            }
        }
        if (result.isSuccess) {
            reload(R.string.health_import_complete)
        } else {
            setResult(R.string.health_import_failed)
        }
    }

    fun clearAll() = runStoreAction {
        store.clearAll()
        ReminderWorker.sync(getApplication(), AppSettings())
    }

    fun healthConnectUnavailableMessage() {
        _state.value = _state.value.copy(
            message = if (healthConnectStatus == HealthConnectClient.SDK_UNAVAILABLE) {
                R.string.health_connect_unavailable
            } else {
                R.string.health_connect_update
            },
        )
    }

    fun permissionDenied() {
        _state.value = _state.value.copy(message = R.string.permission_denied)
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun runStoreAction(action: suspend () -> Unit) = viewModelScope.launch {
        setBusy(true)
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

    private fun setBusy(busy: Boolean) {
        _state.value = _state.value.copy(busy = busy, message = null)
    }

    private fun setResult(@StringRes message: Int) {
        _state.value = _state.value.copy(busy = false, message = message)
    }

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            if (output.size() + count > BackupCodec.MAX_FILE_BYTES) throw BackupFormatException("Backup is too large")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    override fun onCleared() {
        store.close()
    }
}
