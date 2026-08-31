package com.majkeylab.seliacycles

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

enum class MyCalendarFailure { UNSUPPORTED, DAMAGED, EMPTY }

class MyCalendarFormatException(
    message: String,
    cause: Throwable? = null,
    val failure: MyCalendarFailure = MyCalendarFailure.DAMAGED,
) : Exception(message, cause)

data class MyCalendarContainer(val database: ByteArray, val generation: String?)

object MyCalendarContainerReader {
    const val MAX_DATABASE_BYTES = 10 * 1024 * 1024
    private const val MAX_FILE_BYTES = 20 * 1024 * 1024
    private const val MAX_ENTRIES = 32
    private const val MAX_GENERATION_BYTES = 64 * 1024
    private const val MAX_EXTRACTED_BYTES = 20 * 1024 * 1024
    private val sqliteHeader = "SQLite format 3\u0000".encodeToByteArray()

    fun read(input: InputStream): MyCalendarContainer = try {
        ObjectInputStream(LimitedInputStream(input, MAX_FILE_BYTES)).use { objectInput ->
            if (objectInput.readInt() != -1 || objectInput.readInt() != 1 || objectInput.readInt() != 0) {
                throw MyCalendarFormatException("Unsupported My Calendar metadata", failure = MyCalendarFailure.UNSUPPORTED)
            }
            readZip(objectInput)
        }
    } catch (error: MyCalendarFormatException) {
        throw error
    } catch (error: IOException) {
        throw MyCalendarFormatException("Damaged My Calendar backup", error)
    }

    private fun readZip(input: InputStream): MyCalendarContainer {
        var database: ByteArray? = null
        var generation: String? = null
        var extractedBytes = 0
        val names = mutableSetOf<String>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (names.size == MAX_ENTRIES) throw MyCalendarFormatException("Too many backup entries")
                val name = entry.name.replace('\\', '/').substringAfterLast('/')
                if (name.isEmpty() || !names.add(name)) throw MyCalendarFormatException("Duplicate backup entry")
                val bytes = zip.readBounded(if (name == "1.generation") MAX_GENERATION_BYTES else MAX_DATABASE_BYTES)
                extractedBytes += bytes.size
                if (extractedBytes > MAX_EXTRACTED_BYTES) throw MyCalendarFormatException("Backup expands too large")
                when (name) {
                    "cloud.db" -> database = bytes
                    "1.generation" -> generation = bytes.toString(StandardCharsets.UTF_8).trim()
                }
                zip.closeEntry()
            }
        }
        val verified = database ?: throw MyCalendarFormatException(
            "My Calendar database is missing",
            failure = MyCalendarFailure.UNSUPPORTED,
        )
        if (verified.size < sqliteHeader.size ||
            !verified.copyOfRange(0, sqliteHeader.size).contentEquals(sqliteHeader)
        ) throw MyCalendarFormatException("Invalid My Calendar database", failure = MyCalendarFailure.UNSUPPORTED)
        return MyCalendarContainer(verified, generation)
    }

    private fun InputStream.readBounded(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val count = read(buffer)
            if (count == -1) break
            if (output.size() + count > limit) throw MyCalendarFormatException("Backup entry is too large")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private class LimitedInputStream(input: InputStream, private val limit: Int) : FilterInputStream(input) {
        private var count = 0

        override fun read(): Int = super.read().also { if (it != -1) add(1) }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            super.read(buffer, offset, length).also { if (it > 0) add(it) }

        private fun add(value: Int) {
            count += value
            if (count > limit) throw IOException("Backup file is too large")
        }
    }
}

data class MyCalendarPeriodRow(val date: Int, val periodValue: Int)

data class MyCalendarNoteRow(
    val date: Int,
    val weightKg: Double? = null,
    val temperatureC: Double? = null,
    val sleep: String = "",
    val note: String = "",
    val intimate: Int = 0,
    val condom: Int = 0,
    val moodCodes: String = "",
    val symptomCodes: String = "",
    val cervicalFluid: String = "",
    val pregnancyTest: Int = 0,
    val ovulationTest: Int = 0,
    val fertilityTest: Int = 0,
)

data class MyCalendarPreview(
    val logs: List<DayLog>,
    val firstDay: LocalDate,
    val lastDay: LocalDate,
    val unsupportedDetails: Int,
    val generation: String?,
    val seliaTransfer: SeliaTransfer? = null,
)

object MyCalendarTransformer {
    fun transform(
        generation: String?,
        periods: List<MyCalendarPeriodRow>,
        notes: List<MyCalendarNoteRow>,
    ): MyCalendarPreview {
        val logs = mutableMapOf<LocalDate, DayLog>()
        var unsupported = 0
        periods.forEach { row ->
            if (row.periodValue == 0) return@forEach
            val start = parseDate(row.date)
            val periodLength = kotlin.math.abs(row.periodValue)
            if (periodLength !in 1..14) throw MyCalendarFormatException("Invalid period length")
            repeat(periodLength) { offset ->
                val day = start.plusDays(offset.toLong())
                if (day > DayLog.MAX_DATE) throw MyCalendarFormatException("Period date is out of range")
                add(logs, DayLog(day, bleeding = true, flow = Flow.UNKNOWN))
            }
        }
        notes.forEach { row ->
            if (row.note.length > DayLog.MAX_NOTE_LENGTH) throw MyCalendarFormatException("Imported note is too long")
            val legacy = mutableListOf<String>()
            fun preserve(name: String, value: String) {
                if (value.isNotBlank()) {
                    legacy += "$name=$value"
                    unsupported++
                }
            }
            val weight = supportedMeasurement(row.weightKg, DayLog.MIN_WEIGHT_KG..DayLog.MAX_WEIGHT_KG) {
                preserve("weight", row.weightKg.toString())
            }
            val temperature = supportedMeasurement(
                row.temperatureC,
                DayLog.MIN_TEMPERATURE_C..DayLog.MAX_TEMPERATURE_C,
            ) { preserve("temperature", row.temperatureC.toString()) }
            val sleep = row.sleep.takeIf(String::isNotBlank)?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..24.0 }
            if (row.sleep.isNotBlank() && sleep == null) preserve("sleep", row.sleep)
            preserve("mood", row.moodCodes)
            preserve("symptoms", row.symptomCodes)
            preserve("cervicalFluid", row.cervicalFluid)
            if (row.pregnancyTest != 0) preserve("pregnancyTest", row.pregnancyTest.toString())
            if (row.ovulationTest != 0) preserve("ovulationTest", row.ovulationTest.toString())
            if (row.fertilityTest != 0) preserve("fertilityTest", row.fertilityTest.toString())
            if (row.condom != 0 && row.intimate == 0) preserve("condom", row.condom.toString())
            val details = legacy.joinToString(";")
            if (details.length > DayLog.MAX_IMPORTED_DETAILS_LENGTH) {
                throw MyCalendarFormatException("Imported details are too long")
            }
            val log = DayLog(
                day = parseDate(row.date),
                note = row.note,
                weightKg = weight,
                temperatureC = temperature,
                sleepHours = sleep,
                intimacy = when {
                    row.intimate <= 0 -> null
                    row.condom > 0 -> Intimacy.PROTECTED
                    else -> Intimacy.SEX
                },
                importedDetails = details,
            )
            if (!log.isEmpty) add(logs, log)
        }
        if (logs.isEmpty()) throw MyCalendarFormatException(
            "No supported My Calendar records",
            failure = MyCalendarFailure.EMPTY,
        )
        val sorted = logs.values.sortedBy(DayLog::day)
        return MyCalendarPreview(sorted, sorted.first().day, sorted.last().day, unsupported, generation)
    }

    private fun add(logs: MutableMap<LocalDate, DayLog>, log: DayLog) {
        logs[log.day] = logs[log.day]?.let { mergeDayLogs(it, log) } ?: log
        if (logs.size > CycleBackup.MAX_LOGS) throw MyCalendarFormatException("Too many imported records")
    }

    private fun parseDate(value: Int): LocalDate = try {
        value.toString().takeIf { it.length == 8 }?.let { LocalDate.parse(it, DateTimeFormatter.BASIC_ISO_DATE) }
            ?.takeIf { it in DayLog.MIN_DATE..DayLog.MAX_DATE }
            ?: throw MyCalendarFormatException("Invalid My Calendar date")
    } catch (error: DateTimeException) {
        throw MyCalendarFormatException("Invalid My Calendar date", error)
    }

    private inline fun supportedMeasurement(
        value: Double?,
        range: ClosedFloatingPointRange<Double>,
        onUnsupported: () -> Unit,
    ): Double? = when {
        value == null || value == 0.0 -> null
        value.isFinite() && value in range -> value
        else -> {
            onUnsupported()
            null
        }
    }
}

class MyCalendarImporter(context: Context) {
    private val cacheDirectory = context.applicationContext.cacheDir

    fun inspect(input: InputStream): MyCalendarPreview {
        val container = MyCalendarContainerReader.read(input)
        val databaseFile = kotlin.io.path.createTempFile(cacheDirectory.toPath(), "my-calendar-", ".db").toFile()
        return try {
            databaseFile.outputStream().use { it.write(container.database) }
            SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                requireColumns(database, "Period", PERIOD_COLUMNS.toSet())
                requireColumns(database, "Note", NOTE_COLUMNS.toSet())
                readSeliaTransfer(database)?.let { transfer ->
                    val logs = transfer.backup.logs.sortedBy(DayLog::day)
                    if (logs.isEmpty()) throw MyCalendarFormatException(
                        "No Selia records",
                        failure = MyCalendarFailure.EMPTY,
                    )
                    MyCalendarPreview(
                        logs = logs,
                        firstDay = logs.first().day,
                        lastDay = logs.last().day,
                        unsupportedDetails = 0,
                        generation = container.generation,
                        seliaTransfer = transfer,
                    )
                } ?: MyCalendarTransformer.transform(
                    container.generation,
                    readPeriods(database),
                    readNotes(database),
                )
            }
        } catch (error: MyCalendarFormatException) {
            throw error
        } catch (error: Exception) {
            throw MyCalendarFormatException("Cannot read My Calendar database", error)
        } finally {
            if (databaseFile.exists() && !databaseFile.delete()) {
                runCatching { databaseFile.outputStream().use { } }
                databaseFile.delete()
            }
        }
    }

    private fun readPeriods(database: SQLiteDatabase): List<MyCalendarPeriodRow> = database.query(
        "Period",
        PERIOD_COLUMNS,
        null,
        null,
        null,
        null,
        "date ASC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                if (size == MAX_SOURCE_ROWS) throw MyCalendarFormatException("Too many My Calendar rows")
                add(MyCalendarPeriodRow(cursor.getIntExact(0), cursor.getIntExact(1)))
            }
        }
    }

    private fun readNotes(database: SQLiteDatabase): List<MyCalendarNoteRow> = database.query(
        "Note",
        NOTE_COLUMNS,
        null,
        null,
        null,
        null,
        "date ASC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                if (size == MAX_SOURCE_ROWS) throw MyCalendarFormatException("Too many My Calendar rows")
                add(MyCalendarNoteRow(
                    date = cursor.getIntExact(0),
                    weightKg = cursor.getNullableDouble(1),
                    temperatureC = cursor.getNullableDouble(2),
                    sleep = cursor.getString(3).orEmpty(),
                    note = cursor.getString(4).orEmpty(),
                    intimate = cursor.getInt(5),
                    condom = cursor.getInt(6),
                    moodCodes = cursor.getString(7).orEmpty(),
                    symptomCodes = cursor.getString(8).orEmpty(),
                    cervicalFluid = cursor.getString(9).orEmpty(),
                    pregnancyTest = cursor.getInt(10),
                    ovulationTest = cursor.getInt(11),
                    fertilityTest = cursor.getInt(12),
                ))
            }
        }
    }

    private fun requireColumns(database: SQLiteDatabase, table: String, required: Set<String>) {
        val actual = database.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
        }
        if (!actual.containsAll(required)) throw MyCalendarFormatException(
            "Unsupported My Calendar database",
            failure = MyCalendarFailure.UNSUPPORTED,
        )
    }

    private fun readSeliaTransfer(database: SQLiteDatabase): SeliaTransfer? {
        val exists = database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'SeliaBackup'",
            null,
        ).use(Cursor::moveToFirst)
        if (!exists) return null
        return database.query("SeliaBackup", arrayOf("version", "payload"), null, null, null, null, null).use { cursor ->
            if (!cursor.moveToFirst() || cursor.getInt(0) != 1) {
                throw MyCalendarFormatException("Unsupported Selia backup", failure = MyCalendarFailure.UNSUPPORTED)
            }
            val payload = cursor.getBlob(1)
            if (cursor.moveToNext()) throw MyCalendarFormatException(
                "Unsupported Selia backup",
                failure = MyCalendarFailure.UNSUPPORTED,
            )
            SeliaBackupCodec.decode(payload)
        }
    }

    private fun Cursor.getNullableDouble(index: Int): Double? = if (isNull(index)) null else getDouble(index)

    private fun Cursor.getIntExact(index: Int): Int {
        val value = getLong(index)
        if (value !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            throw MyCalendarFormatException("My Calendar number is out of range")
        }
        return value.toInt()
    }

    companion object {
        private const val MAX_SOURCE_ROWS = 20_000
        private val PERIOD_COLUMNS = arrayOf("date", "period")
        private val NOTE_COLUMNS = arrayOf(
            "date",
            "weight",
            "temperature",
            "sleep",
            "note",
            "intimate",
            "condom",
            "mood",
            "symptom",
            "cervical_fluid",
            "pregnancy_test",
            "ovulation_test",
            "fertility_test",
        )
    }
}
