package com.majkeylab.seliacycles

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

internal fun hasLogCapacity(existingCount: Long, replacing: Boolean): Boolean =
    replacing || existingCount < CycleBackup.MAX_LOGS

internal fun mergedTransferSettings(current: AppSettings, incoming: AppSettings): AppSettings =
    incoming.copy(partnerViewEnabled = current.partnerViewEnabled)

class CycleStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE day_logs (
                day INTEGER PRIMARY KEY,
                bleeding INTEGER NOT NULL CHECK (bleeding IN (0, 1)),
                flow TEXT NOT NULL,
                mood TEXT,
                symptoms TEXT NOT NULL,
                note TEXT NOT NULL,
                weight_kg REAL,
                temperature_c REAL,
                sleep_hours REAL,
                intimacy TEXT,
                imported_details TEXT NOT NULL,
                spotting INTEGER NOT NULL CHECK (spotting IN (0, 1)),
                cervical_mucus TEXT,
                ovulation_test TEXT,
                pregnancy_test TEXT,
                pain_level INTEGER CHECK (pain_level BETWEEN 0 AND 10),
                energy TEXT,
                stress TEXT,
                activity TEXT,
                medication TEXT
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE settings (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                cycle_length INTEGER NOT NULL,
                period_length INTEGER NOT NULL,
                first_day TEXT NOT NULL,
                predictions INTEGER NOT NULL CHECK (predictions IN (0, 1)),
                reminder INTEGER NOT NULL CHECK (reminder IN (0, 1)),
                reminder_days INTEGER NOT NULL,
                theme TEXT NOT NULL,
                partner_view INTEGER NOT NULL CHECK (partner_view IN (0, 1)),
                palette TEXT NOT NULL,
                luteal_phase INTEGER NOT NULL,
                custom_primary INTEGER NOT NULL,
                custom_secondary INTEGER NOT NULL,
                custom_tertiary INTEGER NOT NULL,
                custom_entry INTEGER NOT NULL,
                profile_age INTEGER,
                profile_height INTEGER,
                profile_weight REAL,
                profile_goal TEXT NOT NULL,
                life_situation TEXT NOT NULL,
                show_phase_guidance INTEGER NOT NULL CHECK (show_phase_guidance IN (0, 1)),
                show_self_care INTEGER NOT NULL CHECK (show_self_care IN (0, 1)),
                show_cycle_details INTEGER NOT NULL CHECK (show_cycle_details IN (0, 1)),
                simple_mode INTEGER NOT NULL CHECK (simple_mode IN (0, 1)),
                cycle_length_override INTEGER,
                period_length_override INTEGER,
                active_period_start INTEGER
            )
            """.trimIndent(),
        )
        database.insertOrThrow("settings", null, settingsValues(AppSettings()))
        createForecastSnapshotsTable(database)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            database.execSQL("ALTER TABLE day_logs ADD COLUMN weight_kg REAL")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN temperature_c REAL")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN sleep_hours REAL")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN intimacy TEXT")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN imported_details TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            database.execSQL("ALTER TABLE settings ADD COLUMN partner_view INTEGER NOT NULL DEFAULT 0 CHECK (partner_view IN (0, 1))")
            createForecastSnapshotsTable(database)
        }
        if (oldVersion < 4) {
            database.execSQL("ALTER TABLE day_logs ADD COLUMN spotting INTEGER NOT NULL DEFAULT 0 CHECK (spotting IN (0, 1))")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN cervical_mucus TEXT")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN ovulation_test TEXT")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN pregnancy_test TEXT")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN pain_level INTEGER CHECK (pain_level BETWEEN 0 AND 10)")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN energy TEXT")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN stress TEXT")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN activity TEXT")
            database.execSQL("ALTER TABLE day_logs ADD COLUMN medication TEXT")
            database.execSQL("ALTER TABLE settings ADD COLUMN palette TEXT NOT NULL DEFAULT 'SELIA'")
        }
        if (oldVersion < 5) {
            database.execSQL("ALTER TABLE settings ADD COLUMN luteal_phase INTEGER NOT NULL DEFAULT 14")
            database.execSQL("ALTER TABLE settings ADD COLUMN custom_primary INTEGER NOT NULL DEFAULT 16036864")
            database.execSQL("ALTER TABLE settings ADD COLUMN custom_secondary INTEGER NOT NULL DEFAULT 12986408")
            database.execSQL("ALTER TABLE settings ADD COLUMN custom_tertiary INTEGER NOT NULL DEFAULT 35195")
            database.execSQL("ALTER TABLE settings ADD COLUMN profile_age INTEGER")
            database.execSQL("ALTER TABLE settings ADD COLUMN profile_height INTEGER")
            database.execSQL("ALTER TABLE settings ADD COLUMN profile_weight REAL")
            database.execSQL("ALTER TABLE settings ADD COLUMN profile_goal TEXT NOT NULL DEFAULT 'TRACK_CYCLE'")
            database.execSQL("ALTER TABLE settings ADD COLUMN life_situation TEXT NOT NULL DEFAULT 'REGULAR_CYCLES'")
        }
        if (oldVersion < 6) {
            database.execSQL("ALTER TABLE settings ADD COLUMN show_phase_guidance INTEGER NOT NULL DEFAULT 1 CHECK (show_phase_guidance IN (0, 1))")
            database.execSQL("ALTER TABLE settings ADD COLUMN show_self_care INTEGER NOT NULL DEFAULT 1 CHECK (show_self_care IN (0, 1))")
            database.execSQL("ALTER TABLE settings ADD COLUMN show_cycle_details INTEGER NOT NULL DEFAULT 1 CHECK (show_cycle_details IN (0, 1))")
        }
        if (oldVersion < 7) {
            database.execSQL("ALTER TABLE settings ADD COLUMN custom_entry INTEGER NOT NULL DEFAULT 1402304")
        }
        if (oldVersion < 8) {
            database.execSQL("ALTER TABLE settings ADD COLUMN simple_mode INTEGER NOT NULL DEFAULT 0 CHECK (simple_mode IN (0, 1))")
        }
        if (oldVersion < 9) {
            database.execSQL("ALTER TABLE settings ADD COLUMN cycle_length_override INTEGER")
            database.execSQL("ALTER TABLE settings ADD COLUMN period_length_override INTEGER")
            database.execSQL("ALTER TABLE settings ADD COLUMN active_period_start INTEGER")
        }
        if (oldVersion < 10) {
            val today = LocalDate.now()
            database.replaceDayLogs(readLogs(database), today)
            database.execSQL("UPDATE settings SET simple_mode = 0")
            database.execSQL(
                "UPDATE settings SET active_period_start = NULL WHERE active_period_start > ?",
                arrayOf(today.toEpochDay()),
            )
        }
    }

    fun load(): CycleBackup = CycleBackup(
        logs = readLogs(readableDatabase),
        settings = readSettings(readableDatabase),
    )

    fun saveLog(log: DayLog) = writableDatabase.runInTransaction {
        val normalized = PeriodActions.removeFutureBleeding(listOf(log), LocalDate.now()).singleOrNull()
        if (normalized == null || normalized.isEmpty) {
            delete("day_logs", "day = ?", arrayOf(log.day.toEpochDay().toString()))
        } else {
            val day = normalized.day.toEpochDay().toString()
            val replacing = DatabaseUtils.queryNumEntries(this, "day_logs", "day = ?", arrayOf(day)) > 0
            require(hasLogCapacity(DatabaseUtils.queryNumEntries(this, "day_logs"), replacing))
            check(insertWithOnConflict(
                "day_logs",
                null,
                logValues(normalized),
                SQLiteDatabase.CONFLICT_REPLACE,
            ) != -1L)
        }
    }

    fun replaceLogs(logs: List<DayLog>) = writableDatabase.runInTransaction {
        replaceDayLogs(logs)
    }

    fun savePeriodState(logs: List<DayLog>, settings: AppSettings) = writableDatabase.runInTransaction {
        replaceDayLogs(logs)
        check(update("settings", settingsValues(settings.normalized()), "id = 1", null) == 1)
    }

    fun mergeImported(incoming: List<DayLog>) = writableDatabase.runInTransaction {
        val merged = readLogs(this).associateByTo(mutableMapOf(), DayLog::day)
        incoming.forEach { log -> merged[log.day] = merged[log.day]?.let { mergeDayLogs(it, log) } ?: log }
        if (merged.size > CycleBackup.MAX_LOGS) throw IllegalArgumentException("Too many imported records")
        replaceDayLogs(merged.values.toList())
    }

    fun mergeTransfer(transfer: SeliaTransfer) = writableDatabase.runInTransaction {
        val currentSettings = readSettings(this)
        val merged = readLogs(this).associateByTo(mutableMapOf(), DayLog::day)
        transfer.backup.logs.forEach { log -> merged[log.day] = merged[log.day]?.let { mergeDayLogs(it, log) } ?: log }
        if (merged.size > CycleBackup.MAX_LOGS) throw IllegalArgumentException("Too many imported records")
        replaceDayLogs(merged.values.toList())
        val settings = mergedTransferSettings(currentSettings, transfer.backup.settings).normalized()
        check(update("settings", settingsValues(settings), "id = 1", null) == 1)
        transfer.snapshots.forEach { snapshot ->
            insertWithOnConflict(
                "forecast_snapshots",
                null,
                forecastValues(snapshot),
                SQLiteDatabase.CONFLICT_IGNORE,
            )
        }
    }

    fun saveSettings(settings: AppSettings) {
        check(writableDatabase.update("settings", settingsValues(settings.normalized()), "id = 1", null) == 1)
    }

    fun loadForecastSnapshots(): List<ForecastSnapshot> = readableDatabase.query(
        "forecast_snapshots",
        FORECAST_COLUMNS,
        null,
        null,
        null,
        null,
        "month ASC",
    ).use { cursor ->
        buildList(cursor.count) {
            while (cursor.moveToNext()) {
                add(
                    ForecastSnapshot(
                        month = cursor.getLong(0).toYearMonth(),
                        periodStart = LocalDate.ofEpochDay(cursor.getLong(1)),
                        earliestStart = LocalDate.ofEpochDay(cursor.getLong(2)),
                        latestStart = LocalDate.ofEpochDay(cursor.getLong(3)),
                        periodLength = cursor.getInt(4),
                        reconstructed = cursor.getInt(5) == 1,
                    ),
                )
            }
        }
    }.filter { it.month <= YearMonth.now() }

    fun saveForecastSnapshots(snapshots: List<ForecastSnapshot>) = writableDatabase.runInTransaction {
        snapshots.forEach { snapshot ->
            check(insertWithOnConflict(
                "forecast_snapshots",
                null,
                forecastValues(snapshot),
                SQLiteDatabase.CONFLICT_IGNORE,
            ) != -1L)
        }
    }

    fun replace(backup: CycleBackup) = writableDatabase.runInTransaction {
        replaceDayLogs(backup.logs)
        delete("forecast_snapshots", null, null)
        check(update("settings", settingsValues(backup.settings.normalized()), "id = 1", null) == 1)
    }

    fun clearAll() = replace(CycleBackup())

    private fun SQLiteDatabase.replaceDayLogs(logs: List<DayLog>, today: LocalDate = LocalDate.now()) {
        val normalized = PeriodActions.removeFutureBleeding(logs, today).sortedBy(DayLog::day)
        require(normalized.size <= CycleBackup.MAX_LOGS)
        delete("day_logs", null, null)
        normalized.forEach { insertOrThrow("day_logs", null, logValues(it)) }
    }

    private fun AppSettings.normalized(today: LocalDate = LocalDate.now()): AppSettings = copy(
        simpleMode = false,
        activePeriodStart = activePeriodStart?.takeUnless { it.isAfter(today) },
    )

    private fun readLogs(database: SQLiteDatabase): List<DayLog> = database.query(
        "day_logs",
        LOG_COLUMNS,
        null,
        null,
        null,
        null,
        "day ASC",
    ).use { cursor -> buildList(cursor.count) { while (cursor.moveToNext()) add(cursor.toDayLog()) } }

    private fun readSettings(database: SQLiteDatabase): AppSettings = database.query(
        "settings",
        SETTINGS_COLUMNS,
        "id = 1",
        null,
        null,
        null,
        null,
    ).use { cursor ->
        check(cursor.moveToFirst())
        AppSettings(
            cycleLength = cursor.getInt(0),
            periodLength = cursor.getInt(1),
            firstDayOfWeek = DayOfWeek.valueOf(cursor.getString(2)),
            predictionsEnabled = cursor.getInt(3) == 1,
            reminderEnabled = cursor.getInt(4) == 1,
            reminderDays = cursor.getInt(5),
            theme = AppTheme.valueOf(cursor.getString(6)),
            partnerViewEnabled = cursor.getInt(7) == 1,
            palette = AppPalette.valueOf(cursor.getString(8)),
            lutealPhaseLength = cursor.getInt(9),
            customPalette = CustomPalette(cursor.getInt(10), cursor.getInt(11), cursor.getInt(12), cursor.getInt(13)),
            profile = UserProfile(
                age = cursor.getNullableInt(14),
                heightCm = cursor.getNullableInt(15),
                weightKg = cursor.getNullableDouble(16),
                goal = TrackingGoal.valueOf(cursor.getString(17)),
                lifeSituation = LifeSituation.valueOf(cursor.getString(18)),
            ),
            showPhaseGuidance = cursor.getInt(19) == 1,
            showSelfCare = cursor.getInt(20) == 1,
            showCycleDetails = cursor.getInt(21) == 1,
            simpleMode = cursor.getInt(22) == 1,
            cycleLengthOverride = cursor.getNullableInt(23),
            periodLengthOverride = cursor.getNullableInt(24),
            activePeriodStart = cursor.getNullableLong(25)?.let(LocalDate::ofEpochDay),
        )
    }

    private fun Cursor.toDayLog(): DayLog = DayLog(
        day = LocalDate.ofEpochDay(getLong(0)),
        bleeding = getInt(1) == 1,
        flow = Flow.valueOf(getString(2)),
        mood = getString(3)?.let(Mood::valueOf),
        symptoms = getString(4).takeIf(String::isNotEmpty)
            ?.split(',')
            ?.mapTo(mutableSetOf(), Symptom::valueOf)
            .orEmpty(),
        note = getString(5),
        weightKg = getNullableDouble(6),
        temperatureC = getNullableDouble(7),
        sleepHours = getNullableDouble(8),
        intimacy = getString(9)?.let(Intimacy::valueOf),
        importedDetails = getString(10),
        spotting = getInt(11) == 1,
        cervicalMucus = getString(12)?.let(CervicalMucus::valueOf),
        ovulationTest = getString(13)?.let(TestResult::valueOf),
        pregnancyTest = getString(14)?.let(TestResult::valueOf),
        painLevel = getNullableInt(15),
        energy = getString(16)?.let(WellbeingLevel::valueOf),
        stress = getString(17)?.let(WellbeingLevel::valueOf),
        activity = getString(18)?.let(ActivityLevel::valueOf),
        medication = getString(19)?.let(MedicationStatus::valueOf),
    )

    private fun logValues(log: DayLog): ContentValues = ContentValues().apply {
        put("day", log.day.toEpochDay())
        put("bleeding", log.bleeding)
        put("flow", log.flow.name)
        put("mood", log.mood?.name)
        put("symptoms", log.symptoms.map(Symptom::name).sorted().joinToString(","))
        put("note", log.note)
        put("weight_kg", log.weightKg)
        put("temperature_c", log.temperatureC)
        put("sleep_hours", log.sleepHours)
        put("intimacy", log.intimacy?.name)
        put("imported_details", log.importedDetails)
        put("spotting", log.spotting)
        put("cervical_mucus", log.cervicalMucus?.name)
        put("ovulation_test", log.ovulationTest?.name)
        put("pregnancy_test", log.pregnancyTest?.name)
        put("pain_level", log.painLevel)
        put("energy", log.energy?.name)
        put("stress", log.stress?.name)
        put("activity", log.activity?.name)
        put("medication", log.medication?.name)
    }

    private fun Cursor.getNullableDouble(index: Int): Double? = if (isNull(index)) null else getDouble(index)

    private fun Cursor.getNullableInt(index: Int): Int? = if (isNull(index)) null else getInt(index)

    private fun Cursor.getNullableLong(index: Int): Long? = if (isNull(index)) null else getLong(index)

    private fun settingsValues(settings: AppSettings): ContentValues = ContentValues().apply {
        put("id", 1)
        put("cycle_length", settings.cycleLength)
        put("period_length", settings.periodLength)
        put("first_day", settings.firstDayOfWeek.name)
        put("predictions", settings.predictionsEnabled)
        put("reminder", settings.reminderEnabled)
        put("reminder_days", settings.reminderDays)
        put("theme", settings.theme.name)
        put("partner_view", settings.partnerViewEnabled)
        put("palette", settings.palette.name)
        put("luteal_phase", settings.lutealPhaseLength)
        put("custom_primary", settings.customPalette.primaryRgb)
        put("custom_secondary", settings.customPalette.secondaryRgb)
        put("custom_tertiary", settings.customPalette.tertiaryRgb)
        put("custom_entry", settings.customPalette.entryRgb)
        put("profile_age", settings.profile.age)
        put("profile_height", settings.profile.heightCm)
        put("profile_weight", settings.profile.weightKg)
        put("profile_goal", settings.profile.goal.name)
        put("life_situation", settings.profile.lifeSituation.name)
        put("show_phase_guidance", settings.showPhaseGuidance)
        put("show_self_care", settings.showSelfCare)
        put("show_cycle_details", settings.showCycleDetails)
        put("simple_mode", settings.simpleMode)
        put("cycle_length_override", settings.cycleLengthOverride)
        put("period_length_override", settings.periodLengthOverride)
        put("active_period_start", settings.activePeriodStart?.toEpochDay())
    }

    private fun forecastValues(snapshot: ForecastSnapshot): ContentValues = ContentValues().apply {
        put("month", snapshot.month.toEpochMonth())
        put("period_start", snapshot.periodStart.toEpochDay())
        put("earliest_start", snapshot.earliestStart.toEpochDay())
        put("latest_start", snapshot.latestStart.toEpochDay())
        put("period_length", snapshot.periodLength)
        put("reconstructed", snapshot.reconstructed)
    }

    private fun createForecastSnapshotsTable(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE forecast_snapshots (
                month INTEGER PRIMARY KEY,
                period_start INTEGER NOT NULL,
                earliest_start INTEGER NOT NULL,
                latest_start INTEGER NOT NULL,
                period_length INTEGER NOT NULL CHECK (period_length BETWEEN 1 AND 14),
                reconstructed INTEGER NOT NULL CHECK (reconstructed IN (0, 1))
            )
            """.trimIndent(),
        )
    }

    private fun YearMonth.toEpochMonth(): Long = year.toLong() * 12 + monthValue - 1

    private fun Long.toYearMonth(): YearMonth = YearMonth.of(
        Math.floorDiv(this, 12L).toInt(),
        Math.floorMod(this, 12L).toInt() + 1,
    )

    private inline fun <T> SQLiteDatabase.runInTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try {
            block().also { setTransactionSuccessful() }
        } finally {
            endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "selia-cycles.db"
        private const val DATABASE_VERSION = 10
        private val LOG_COLUMNS = arrayOf(
            "day",
            "bleeding",
            "flow",
            "mood",
            "symptoms",
            "note",
            "weight_kg",
            "temperature_c",
            "sleep_hours",
            "intimacy",
            "imported_details",
            "spotting",
            "cervical_mucus",
            "ovulation_test",
            "pregnancy_test",
            "pain_level",
            "energy",
            "stress",
            "activity",
            "medication",
        )
        private val SETTINGS_COLUMNS = arrayOf(
            "cycle_length",
            "period_length",
            "first_day",
            "predictions",
            "reminder",
            "reminder_days",
            "theme",
            "partner_view",
            "palette",
            "luteal_phase",
            "custom_primary",
            "custom_secondary",
            "custom_tertiary",
            "custom_entry",
            "profile_age",
            "profile_height",
            "profile_weight",
            "profile_goal",
            "life_situation",
            "show_phase_guidance",
            "show_self_care",
            "show_cycle_details",
            "simple_mode",
            "cycle_length_override",
            "period_length_override",
            "active_period_start",
        )
        private val FORECAST_COLUMNS = arrayOf(
            "month",
            "period_start",
            "earliest_start",
            "latest_start",
            "period_length",
            "reconstructed",
        )
    }
}
