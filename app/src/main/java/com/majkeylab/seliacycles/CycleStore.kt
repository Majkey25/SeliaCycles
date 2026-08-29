package com.majkeylab.seliacycles

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

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
                palette TEXT NOT NULL
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
    }

    fun load(): CycleBackup = CycleBackup(
        logs = readLogs(readableDatabase),
        settings = readSettings(readableDatabase),
    )

    fun saveLog(log: DayLog) {
        if (log.isEmpty) {
            writableDatabase.delete("day_logs", "day = ?", arrayOf(log.day.toEpochDay().toString()))
        } else {
            check(writableDatabase.insertWithOnConflict(
                "day_logs",
                null,
                logValues(log),
                SQLiteDatabase.CONFLICT_REPLACE,
            ) != -1L)
        }
    }

    fun saveSettings(settings: AppSettings) {
        check(writableDatabase.update("settings", settingsValues(settings), "id = 1", null) == 1)
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
    }

    fun saveForecastSnapshots(snapshots: List<ForecastSnapshot>) = writableDatabase.runInTransaction {
        snapshots.forEach { snapshot ->
            check(insertWithOnConflict(
                "forecast_snapshots",
                null,
                ContentValues().apply {
                    put("month", snapshot.month.toEpochMonth())
                    put("period_start", snapshot.periodStart.toEpochDay())
                    put("earliest_start", snapshot.earliestStart.toEpochDay())
                    put("latest_start", snapshot.latestStart.toEpochDay())
                    put("period_length", snapshot.periodLength)
                    put("reconstructed", snapshot.reconstructed)
                },
                SQLiteDatabase.CONFLICT_IGNORE,
            ) != -1L)
        }
    }

    fun replace(backup: CycleBackup) = writableDatabase.runInTransaction {
        delete("day_logs", null, null)
        delete("forecast_snapshots", null, null)
        backup.logs.forEach { insertOrThrow("day_logs", null, logValues(it)) }
        check(update("settings", settingsValues(backup.settings), "id = 1", null) == 1)
    }

    fun clearAll() = replace(CycleBackup())

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
        private const val DATABASE_VERSION = 4
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
