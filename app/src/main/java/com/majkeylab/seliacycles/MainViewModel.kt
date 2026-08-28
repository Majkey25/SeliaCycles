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

data class CloudState(
    val available: Boolean = false,
    val account: CloudAccount? = null,
    val syncEnabled: Boolean = false,
    val busy: Boolean = false,
    val inviteToken: String? = null,
    val partnerCalendars: List<PartnerCalendar> = emptyList(),
    val readerUids: List<String> = emptyList(),
    val selectedPartnerUid: String? = null,
)

data class AppState(
    val backup: CycleBackup = CycleBackup(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val myCalendarPreview: MyCalendarPreview? = null,
    val cloud: CloudState = CloudState(),
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
    private val myCalendarImporter = MyCalendarImporter(application)
    private val calendarSync = CalendarSyncRepository(application)
    private val cloudPreferences = application.getSharedPreferences("cloud-sync", 0)
    private val storeMutex = Mutex()
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()

    val healthConnectStatus: Int
        get() = healthConnect.status

    init {
        reload()
    }

    fun saveLog(log: DayLog) = runStoreAction(syncAfter = true) {
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
            syncIfEnabled()
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
            syncIfEnabled()
            reload(R.string.health_import_complete)
        } else {
            setResult(R.string.health_import_failed)
        }
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
                setResult(when ((error as? MyCalendarFormatException)?.failure) {
                    MyCalendarFailure.UNSUPPORTED -> R.string.my_calendar_import_unsupported
                    MyCalendarFailure.EMPTY -> R.string.my_calendar_import_empty
                    MyCalendarFailure.DAMAGED, null -> R.string.my_calendar_import_damaged
                })
            },
        )
    }

    fun confirmMyCalendarImport() {
        val preview = _state.value.myCalendarPreview ?: return
        runStoreAction(R.string.my_calendar_import_complete, syncAfter = true) {
            store.mergeImported(preview.logs)
        }
    }

    fun cancelMyCalendarImport() {
        _state.value = _state.value.copy(myCalendarPreview = null)
    }

    fun clearAll() = runStoreAction(syncAfter = true) {
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

    fun initializeCloud(available: Boolean, account: CloudAccount?) {
        val enabled = cloudPreferences.getBoolean("enabled", false) && account != null
        _state.value = _state.value.copy(cloud = CloudState(
            available = available,
            account = account,
            syncEnabled = enabled,
        ))
        if (account != null) refreshCloud()
    }

    fun signIn(action: suspend () -> CloudAccount) = viewModelScope.launch {
        updateCloud { it.copy(busy = true) }
        runCatching { action() }.fold(
            onSuccess = { account ->
                updateCloud { it.copy(account = account, busy = false) }
                if (_state.value.cloud.syncEnabled) syncIfEnabled()
                refreshCloudData()
            },
            onFailure = {
                updateCloud { it.copy(busy = false) }
                setResult(R.string.cloud_sign_in_failed)
            },
        )
    }

    fun signOut(action: suspend () -> Unit) = viewModelScope.launch {
        updateCloud { it.copy(busy = true) }
        runCatching { action() }.fold(
            onSuccess = {
                cloudPreferences.edit().putBoolean("enabled", false).apply()
                _state.value = _state.value.copy(cloud = CloudState(available = _state.value.cloud.available))
            },
            onFailure = {
                updateCloud { it.copy(busy = false) }
                setResult(R.string.cloud_operation_failed)
            },
        )
    }

    fun setCloudSyncEnabled(enabled: Boolean) = viewModelScope.launch {
        if (!enabled) {
            cloudPreferences.edit().putBoolean("enabled", false).apply()
            updateCloud { it.copy(syncEnabled = false, busy = false) }
            return@launch
        }
        updateCloud { it.copy(busy = true) }
        val result = runCatching { syncOwnerNow() }
        if (result.isSuccess) {
            cloudPreferences.edit().putBoolean("enabled", true).apply()
            updateCloud { it.copy(syncEnabled = true, busy = false) }
            setResult(R.string.cloud_sync_complete)
        } else {
            updateCloud { it.copy(syncEnabled = false, busy = false) }
            setResult(R.string.cloud_operation_failed)
        }
    }

    fun syncNow() = viewModelScope.launch {
        updateCloud { it.copy(busy = true) }
        if (runCatching { syncOwnerNow() }.isSuccess) {
            updateCloud { it.copy(busy = false) }
            setResult(R.string.cloud_sync_complete)
        } else {
            updateCloud { it.copy(busy = false) }
            setResult(R.string.cloud_operation_failed)
        }
    }

    fun createPartnerInvitation() = viewModelScope.launch {
        updateCloud { it.copy(busy = true, inviteToken = null) }
        runCatching { calendarSync.createInvitation() }.fold(
            onSuccess = { token ->
                updateCloud { it.copy(busy = false, inviteToken = token) }
                setResult(R.string.partner_invite_created)
            },
            onFailure = {
                updateCloud { it.copy(busy = false) }
                setResult(R.string.cloud_operation_failed)
            },
        )
    }

    fun acceptPartnerInvitation(token: String) = viewModelScope.launch {
        updateCloud { it.copy(busy = true) }
        if (runCatching { calendarSync.acceptInvitation(token) }.isSuccess) {
            refreshCloudData()
            setResult(R.string.partner_invite_accepted)
        } else {
            updateCloud { it.copy(busy = false) }
            setResult(R.string.partner_invite_failed)
        }
    }

    fun revokePartner(readerUid: String) = viewModelScope.launch {
        updateCloud { it.copy(busy = true) }
        if (runCatching { calendarSync.revoke(readerUid) }.isSuccess) {
            refreshCloudData()
            setResult(R.string.partner_revoked)
        } else {
            updateCloud { it.copy(busy = false) }
            setResult(R.string.cloud_operation_failed)
        }
    }

    fun deleteCloudCopy() = viewModelScope.launch {
        updateCloud { it.copy(busy = true) }
        if (runCatching { calendarSync.deleteCloudCopy() }.isSuccess) {
            cloudPreferences.edit().putBoolean("enabled", false).apply()
            updateCloud { it.copy(
                syncEnabled = false,
                busy = false,
                inviteToken = null,
                readerUids = emptyList(),
            ) }
            setResult(R.string.cloud_copy_deleted)
        } else {
            updateCloud { it.copy(busy = false) }
            setResult(R.string.cloud_operation_failed)
        }
    }

    fun selectPartnerCalendar(ownerUid: String?) {
        updateCloud { cloud ->
            cloud.copy(selectedPartnerUid = ownerUid?.takeIf { candidate ->
                cloud.partnerCalendars.any { it.ownerUid == candidate }
            })
        }
    }

    fun refreshCloud() = viewModelScope.launch {
        updateCloud { it.copy(busy = true) }
        if (runCatching { refreshCloudData() }.isFailure) {
            updateCloud { it.copy(busy = false) }
            setResult(R.string.cloud_operation_failed)
        }
    }

    private fun runStoreAction(
        @StringRes successMessage: Int? = null,
        syncAfter: Boolean = false,
        action: suspend () -> Unit,
    ) = viewModelScope.launch {
        setBusy(true)
        val result = runCatching { withContext(Dispatchers.IO) { storeMutex.withLock { action() } } }
        if (result.isSuccess && syncAfter) syncIfEnabled()
        reload(if (result.isFailure) R.string.operation_failed else successMessage)
    }

    private suspend fun syncIfEnabled() {
        if (_state.value.cloud.syncEnabled && _state.value.cloud.account != null) {
            runCatching { syncOwnerNow() }
        }
    }

    private suspend fun syncOwnerNow() {
        val account = requireNotNull(_state.value.cloud.account)
        val logs = withContext(Dispatchers.IO) { storeMutex.withLock { store.load().logs } }
        calendarSync.syncOwner(logs, account.displayName ?: "Selia Cycles")
    }

    private suspend fun refreshCloudData() {
        if (_state.value.cloud.account == null) return
        val partners = calendarSync.partnerCalendars()
        val readers = calendarSync.readers()
        updateCloud { current -> current.copy(
            busy = false,
            partnerCalendars = partners,
            readerUids = readers,
            selectedPartnerUid = current.selectedPartnerUid?.takeIf { selected ->
                partners.any { it.ownerUid == selected }
            },
        ) }
    }

    private fun updateCloud(transform: (CloudState) -> CloudState) {
        _state.value = _state.value.copy(cloud = transform(_state.value.cloud))
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
            onSuccess = {
                _state.value.copy(
                    backup = it,
                    loading = false,
                    busy = false,
                    myCalendarPreview = null,
                    message = message,
                )
            },
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
