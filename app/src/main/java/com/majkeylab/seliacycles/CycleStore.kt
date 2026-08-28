package com.majkeylab.seliacycles

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.DayOfWeek
import java.time.LocalDate

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
                note TEXT NOT NULL
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
                theme TEXT NOT NULL
            )
            """.trimIndent(),
        )
        database.insertOrThrow("settings", null, settingsValues(AppSettings()))
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

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

    fun replace(backup: CycleBackup) = writableDatabase.runInTransaction {
        delete("day_logs", null, null)
        backup.logs.forEach { insertOrThrow("day_logs", null, logValues(it)) }
        check(update("settings", settingsValues(backup.settings), "id = 1", null) == 1)
    }

    fun clearAll() = replace(CycleBackup())

    fun mergeImported(imported: List<DayLog>) = writableDatabase.runInTransaction {
        val merged = readLogs(this).associateByTo(mutableMapOf(), DayLog::day)
        imported.forEach { incoming ->
            val current = merged[incoming.day]
            merged[incoming.day] = if (current == null) {
                incoming
            } else {
                current.copy(
                    bleeding = current.bleeding || incoming.bleeding,
                    flow = when {
                        incoming.flow != Flow.UNKNOWN -> incoming.flow
                        current.flow != Flow.NONE -> current.flow
                        else -> Flow.UNKNOWN
                    },
                )
            }
        }
        require(merged.size <= CycleBackup.MAX_LOGS)
        imported.map(DayLog::day).distinct().forEach { day ->
            check(insertWithOnConflict(
                "day_logs",
                null,
                logValues(merged.getValue(day)),
                SQLiteDatabase.CONFLICT_REPLACE,
            ) != -1L)
        }
    }

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
    )

    private fun logValues(log: DayLog): ContentValues = ContentValues().apply {
        put("day", log.day.toEpochDay())
        put("bleeding", log.bleeding)
        put("flow", log.flow.name)
        put("mood", log.mood?.name)
        put("symptoms", log.symptoms.map(Symptom::name).sorted().joinToString(","))
        put("note", log.note)
    }

    private fun settingsValues(settings: AppSettings): ContentValues = ContentValues().apply {
        put("id", 1)
        put("cycle_length", settings.cycleLength)
        put("period_length", settings.periodLength)
        put("first_day", settings.firstDayOfWeek.name)
        put("predictions", settings.predictionsEnabled)
        put("reminder", settings.reminderEnabled)
        put("reminder_days", settings.reminderDays)
        put("theme", settings.theme.name)
    }

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
        private const val DATABASE_VERSION = 1
        private val LOG_COLUMNS = arrayOf("day", "bleeding", "flow", "mood", "symptoms", "note")
        private val SETTINGS_COLUMNS = arrayOf(
            "cycle_length",
            "period_length",
            "first_day",
            "predictions",
            "reminder",
            "reminder_days",
            "theme",
        )
    }
}
