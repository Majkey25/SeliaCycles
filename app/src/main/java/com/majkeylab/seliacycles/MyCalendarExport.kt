package com.majkeylab.seliacycles

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

data class SeliaTransfer(
    val backup: CycleBackup,
    val snapshots: List<ForecastSnapshot>,
)

object MyCalendarExportMapper {
    fun periodRows(logs: List<DayLog>): List<MyCalendarPeriodRow> = logs.asSequence()
        .filter(DayLog::bleeding)
        .map(DayLog::day)
        .sorted()
        .fold(mutableListOf<MutableList<LocalDate>>()) { periods, day ->
            val current = periods.lastOrNull()
            if (current == null || ChronoUnit.DAYS.between(current.last(), day) > 1) {
                periods += mutableListOf(day)
            } else {
                current += day
            }
            periods
        }.map { period -> MyCalendarPeriodRow(period.first().toBasicDate(), period.size) }

    fun noteRows(logs: List<DayLog>): List<MyCalendarNoteRow> = logs.mapNotNull { log ->
        val hasCommonValue = log.note.isNotBlank() || log.weightKg != null || log.temperatureC != null ||
            log.sleepHours != null || log.intimacy != null
        if (!hasCommonValue) return@mapNotNull null
        MyCalendarNoteRow(
            date = log.day.toBasicDate(),
            weightKg = log.weightKg,
            temperatureC = log.temperatureC,
            sleep = log.sleepHours?.toString().orEmpty(),
            note = log.note,
            intimate = if (log.intimacy == null) 0 else 1,
            condom = if (log.intimacy == Intimacy.PROTECTED) 1 else 0,
        )
    }

    private fun LocalDate.toBasicDate(): Int = format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
}

class MyCalendarExporter(context: Context) {
    private val cacheDirectory = context.applicationContext.cacheDir

    fun write(transfer: SeliaTransfer, output: OutputStream) {
        require(transfer.backup.logs.isNotEmpty())
        val prediction = CyclePredictor.predict(
            bleedingDays = transfer.backup.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day),
            defaultCycleLength = transfer.backup.settings.cycleLength,
            defaultPeriodLength = transfer.backup.settings.periodLength,
            cycleLengthOverride = transfer.backup.settings.cycleLengthOverride,
            periodLengthOverride = transfer.backup.settings.periodLengthOverride,
            activePeriodStart = transfer.backup.settings.activePeriodStart,
        )
        val databaseFile = kotlin.io.path.createTempFile(cacheDirectory.toPath(), "selia-export-", ".db").toFile()
        try {
            SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
                createSchema(database)
                database.delete("android_metadata", null, null)
                database.insertOrThrow("android_metadata", null, ContentValues().apply { put("locale", Locale.getDefault().toLanguageTag()) })
                database.insertOrThrow("User", null, ContentValues().apply {
                    put("uid", MY_CALENDAR_UID)
                    put("update_time", System.currentTimeMillis())
                    put("name", "Selia Cycles")
                    put("setting", userSettings(transfer.backup.settings, prediction).toString())
                })
                MyCalendarExportMapper.periodRows(transfer.backup.logs).forEach { row ->
                    database.insertOrThrow("Period", null, ContentValues().apply {
                        put("uid", MY_CALENDAR_UID)
                        put("update_time", System.currentTimeMillis())
                        put("create_date", row.date)
                        put("date", row.date)
                        put("period", row.periodValue)
                        put("cycle", prediction.averageCycleLength)
                        put("pregnancy", 0)
                        put("pregnancy_date", 0)
                        put("due_date_select", 0)
                        put("mask_periods", "[]")
                    })
                }
                MyCalendarExportMapper.noteRows(transfer.backup.logs).forEach { row ->
                    database.insertOrThrow("Note", null, ContentValues().apply {
                        put("date", row.date)
                        put("uid", MY_CALENDAR_UID)
                        put("update_time", System.currentTimeMillis())
                        put("create_date", row.date)
                        put("temperature", row.temperatureC)
                        put("weight", row.weightKg)
                        put("sleep", row.sleep)
                        put("note", row.note)
                        put("intimate", row.intimate)
                        put("condom", row.condom)
                    })
                }
                database.insertOrThrow("SeliaBackup", null, ContentValues().apply {
                    put("version", 1)
                    put("payload", SeliaBackupCodec.encode(transfer))
                })
            }
            if (databaseFile.length() > MyCalendarContainerReader.MAX_DATABASE_BYTES) {
                throw MyCalendarFormatException("Export database is too large")
            }
            MyCalendarContainerWriter.write(databaseFile.readBytes(), output, sidecars(transfer, prediction))
        } finally {
            listOf(databaseFile, databaseFile.resolveSibling("${databaseFile.name}-journal")).forEach { file ->
                if (file.exists() && !file.delete()) {
                    file.outputStream().use { }
                    file.delete()
                }
            }
        }
    }

    private fun sidecars(transfer: SeliaTransfer, prediction: CyclePrediction): Map<String, ByteArray> {
        val settings = transfer.backup.settings
        val now = System.currentTimeMillis()
        val periods = JSONArray()
        MyCalendarExportMapper.periodRows(transfer.backup.logs).forEach { row ->
            val date = row.date.toLocalDate()
            periods.put(JSONObject()
                .put("start", date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .put("period", row.periodValue)
                .put("cycle", prediction.averageCycleLength)
                .put("pregnancy", false)
                .put("uid", MY_CALENDAR_UID)
                .put("pregnancy_date", 0)
                .put("due_date_select", 0)
                .put("dueDateSelect", 0)
                .put("update_time", now)
                .put("updateTime", now)
                .put("date_str", date.toString())
                .put("createDate", now)
                .put("maskPeriods", "[]")
                .put("temp2", "")
                .put("temp3", ""))
        }
        val result = linkedMapOf(
            "1.user" to encryptedMyCalendarJson(JSONArray().put(JSONObject()
                .put("answer", "")
                .put("email", "")
                .put("password", "")
                .put("question", "")
                .put("setting", userSettings(settings, prediction).toString())
                .put("uid", MY_CALENDAR_UID)
                .put("name", "Selia Cycles")
                .put("pwdType", 0)
                .put("avatarUrl", "")
                .put("firebaseName", "")
                .put("temp2", "")
                .put("temp3", "")).toString()),
            "1.period" to encryptedMyCalendarJson(periods.toString()),
            "1.pill" to encryptedMyCalendarJson("[]"),
            "1.pill_record" to encryptedMyCalendarJson("[]"),
        )
        MyCalendarExportMapper.noteRows(transfer.backup.logs).chunked(NOTES_PER_FILE)
            .ifEmpty { listOf(emptyList()) }
            .forEachIndexed { index, rows -> result["${index + 1}.note"] = encryptedMyCalendarJson(noteJson(rows, now).toString()) }
        return result
    }

    private fun noteJson(rows: List<MyCalendarNoteRow>, now: Long): JSONArray = JSONArray().apply {
        rows.forEach { row ->
            val date = row.date.toLocalDate()
            put(JSONObject()
                .put("date", date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .put("height", 0)
                .put("temperature", row.temperatureC ?: 0.0)
                .put("uid", MY_CALENDAR_UID)
                .put("weight", row.weightKg ?: 0.0)
                .put("condom", row.condom > 0)
                .put("mood", "")
                .put("note", row.note)
                .put("pill", "")
                .put("symptom", "")
                .put("intimate", row.intimate > 0)
                .put("ovulation_test", 0)
                .put("fertilityTest", 0)
                .put("pregnancyTest", 0)
                .put("cervicalFluid", "0,0,0,0,0,0,")
                .put("lastCMInput", "0:0")
                .put("lastTestInput", "0:0")
                .put("cervicalPosition", 0)
                .put("cervicalTexture", 0)
                .put("cervix", 0)
                .put("pill_new", "")
                .put("frequencyTaken", "")
                .put("sextimes", if (row.intimate > 0) 1 else 0)
                .put("noSex", 0)
                .put("organsm", 0)
                .put("date_str", date.toString())
                .put("creatDate", now)
                .put("masturbate", 0)
                .put("condom_int", row.condom)
                .put("waist", 0)
                .put("neck", 0)
                .put("hip", 0)
                .put("cloud_uid", MY_CALENDAR_UID)
                .put("neWeight", "")
                .put("sleep", row.sleep)
                .put("lochia", 0)
                .put("breast", "")
                .put("workout", ""))
        }
    }

    private fun userSettings(settings: AppSettings, prediction: CyclePrediction): JSONObject = JSONObject()
        .put("1", JSONArray()
            .put(JSONObject().put("key", "menses_length").put("value", prediction.averagePeriodLength))
            .put(JSONObject().put("key", "period_length").put("value", prediction.averageCycleLength)))
        .put("2", JSONArray())
        .put("3", JSONArray()
            .put(JSONObject().put("key", "show_predict_period").put("value", settings.predictionsEnabled))
            .put(JSONObject().put("key", "is_pregnant").put("value", settings.profile.lifeSituation == LifeSituation.PREGNANT)))
        .put("4", JSONArray())
        .put("5", JSONArray())
        .put("6", JSONArray())

    private fun Int.toLocalDate(): LocalDate = LocalDate.parse(toString(), DateTimeFormatter.BASIC_ISO_DATE)

    private fun createSchema(database: SQLiteDatabase) {
        listOf(
            "CREATE TABLE User (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uid INTEGER, update_time INTEGER, name TEXT, setting TEXT, temp1 TEXT, temp2 TEXT)",
            "CREATE TABLE Period (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uid INTEGER, update_time INTEGER, create_date INTEGER, date INTEGER, period INTEGER, cycle INTEGER, pregnancy INTEGER, pregnancy_date INTEGER, due_date_select INTEGER, mask_periods TEXT, temp1 TEXT, temp2 TEXT)",
            "CREATE TABLE Note (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER, uid INTEGER, update_time INTEGER, create_date INTEGER, height REAL, temperature REAL, weight REAL, symptom TEXT, mood TEXT, note TEXT, pill TEXT, intimate INTEGER, condom INTEGER, sextimes INTEGER, organsm INTEGER, ovulation_test INTEGER, fertility_test INTEGER, pregnancy_test INTEGER, cervical_fluid TEXT, cervical_position INTEGER, cervical_texture INTEGER, cervix INTEGER, water TEXT, masturbate INTEGER, waist REAL, neck REAL, hip REAL, more_weight TEXT, sleep TEXT, breast TEXT, workout TEXT, temp1 TEXT, temp2 TEXT)",
            "CREATE TABLE Pill (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uid INTEGER, pid INTEGER, update_time INTEGER, name TEXT, classify INTEGER, pill_extension_json TEXT, notification_switch INTEGER, start_date INTEGER, end_date INTEGER, pill_type INTEGER, pill_type_json TEXT)",
            "CREATE TABLE note_field_meta (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uid INTEGER NOT NULL, date INTEGER NOT NULL, field TEXT NOT NULL, update_time INTEGER NOT NULL DEFAULT 0, item_timestamps TEXT)",
            "CREATE UNIQUE INDEX idx_field_meta_uid_date_field ON note_field_meta (uid, date, field)",
            "CREATE TABLE SeliaBackup (version INTEGER PRIMARY KEY NOT NULL, payload BLOB NOT NULL)",
        ).forEach(database::execSQL)
    }

    private companion object {
        const val MY_CALENDAR_UID = 0
        const val NOTES_PER_FILE = 100
    }
}

object MyCalendarContainerWriter {
    fun write(database: ByteArray, output: OutputStream, sidecars: Map<String, ByteArray> = emptyMap()) {
        require(database.size <= MyCalendarContainerReader.MAX_DATABASE_BYTES)
        ObjectOutputStream(output).use { objectOutput ->
            objectOutput.writeInt(-1)
            objectOutput.writeInt(1)
            objectOutput.writeInt(0)
            ZipOutputStream(objectOutput).use { zip ->
                val now = ZonedDateTime.now()
                val entries = linkedMapOf(
                    "1.timezone" to "{\"displayName\":\"${ZoneId.systemDefault().id}\"}".encodeToByteArray(),
                    "1.generation" to "7".encodeToByteArray(),
                    "1.info" to "7 _ SeliaCycles_Time_${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT))}"
                        .encodeToByteArray(),
                    "cloud.db" to database,
                )
                entries.putAll(sidecars.ifEmpty { mapOf(
                    "1.user" to encryptedMyCalendarJson("[]"),
                    "1.period" to encryptedMyCalendarJson("[]"),
                    "1.pill" to encryptedMyCalendarJson("[]"),
                    "1.note" to encryptedMyCalendarJson("[]"),
                    "1.pill_record" to encryptedMyCalendarJson("[]"),
                ) })
                entries.forEach { (name, value) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(value)
                    zip.closeEntry()
                }
            }
        }
    }
}

internal fun encryptedMyCalendarJson(json: String): ByteArray {
    val value = json.toCharArray()
    val key = "Period"
    for (index in value.indices step 2) {
        value[index] = (value[index].code xor key[(index / 2) % key.length].code).toChar()
    }
    return String(value).encodeToByteArray()
}

object SeliaBackupCodec {
    private const val MAGIC = 0x53434C31
    private const val VERSION = 1
    const val MAX_BYTES = 5 * 1024 * 1024

    fun encode(transfer: SeliaTransfer): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeSettings(transfer.backup.settings)
            output.writeInt(transfer.backup.logs.size)
            transfer.backup.logs.sortedBy(DayLog::day).forEach { output.writeLog(it) }
            output.writeInt(transfer.snapshots.size)
            transfer.snapshots.sortedBy(ForecastSnapshot::month).forEach { output.writeSnapshot(it) }
        }
        return bytes.toByteArray().also { require(it.size <= MAX_BYTES) }
    }

    fun decode(bytes: ByteArray): SeliaTransfer {
        if (bytes.size > MAX_BYTES) throw MyCalendarFormatException("Selia backup is too large")
        return try {
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                    throw MyCalendarFormatException("Unsupported Selia backup", failure = MyCalendarFailure.UNSUPPORTED)
                }
                val settings = input.readSettings()
                val logCount = input.readCount(CycleBackup.MAX_LOGS)
                val logs = List(logCount) { input.readLog() }
                val snapshotCount = input.readCount(CalendarPaging.pageCount)
                val snapshots = List(snapshotCount) { input.readSnapshot() }
                if (input.read() != -1) throw MyCalendarFormatException("Trailing Selia backup data")
                SeliaTransfer(CycleBackup(logs, settings), snapshots)
            }
        } catch (error: MyCalendarFormatException) {
            throw error
        } catch (error: Exception) {
            throw MyCalendarFormatException("Damaged Selia backup", error)
        }
    }

    private fun DataOutputStream.writeSettings(settings: AppSettings) {
        writeInt(settings.cycleLength)
        writeInt(settings.periodLength)
        writeNullableInt(settings.cycleLengthOverride)
        writeNullableInt(settings.periodLengthOverride)
        writeNullableDate(settings.activePeriodStart)
        writeUTF(settings.firstDayOfWeek.name)
        writeBoolean(settings.predictionsEnabled)
        writeBoolean(settings.reminderEnabled)
        writeInt(settings.reminderDays)
        writeInt(settings.lutealPhaseLength)
        writeUTF(settings.theme.name)
        writeUTF(settings.palette.name)
        writeInt(settings.customPalette.primaryRgb)
        writeInt(settings.customPalette.secondaryRgb)
        writeInt(settings.customPalette.tertiaryRgb)
        writeInt(settings.customPalette.entryRgb)
        writeBoolean(settings.partnerViewEnabled)
        writeNullableInt(settings.profile.age)
        writeNullableInt(settings.profile.heightCm)
        writeNullableDouble(settings.profile.weightKg)
        writeUTF(settings.profile.goal.name)
        writeUTF(settings.profile.lifeSituation.name)
        writeBoolean(settings.showPhaseGuidance)
        writeBoolean(settings.showSelfCare)
        writeBoolean(settings.showCycleDetails)
        writeBoolean(settings.simpleMode)
    }

    private fun DataInputStream.readSettings(): AppSettings = AppSettings(
        cycleLength = readInt(),
        periodLength = readInt(),
        cycleLengthOverride = readNullableInt(),
        periodLengthOverride = readNullableInt(),
        activePeriodStart = readNullableDate(),
        firstDayOfWeek = readEnum(DayOfWeek.entries.toTypedArray()),
        predictionsEnabled = readBoolean(),
        reminderEnabled = readBoolean(),
        reminderDays = readInt(),
        lutealPhaseLength = readInt(),
        theme = readEnum(AppTheme.entries.toTypedArray()),
        palette = readEnum(AppPalette.entries.toTypedArray()),
        customPalette = CustomPalette(readInt(), readInt(), readInt(), readInt()),
        partnerViewEnabled = readBoolean(),
        profile = UserProfile(
            age = readNullableInt(),
            heightCm = readNullableInt(),
            weightKg = readNullableDouble(),
            goal = readEnum(TrackingGoal.entries.toTypedArray()),
            lifeSituation = readEnum(LifeSituation.entries.toTypedArray()),
        ),
        showPhaseGuidance = readBoolean(),
        showSelfCare = readBoolean(),
        showCycleDetails = readBoolean(),
        simpleMode = readBoolean(),
    )

    private fun DataOutputStream.writeLog(log: DayLog) {
        writeLong(log.day.toEpochDay())
        writeBoolean(log.bleeding)
        writeBoolean(log.spotting)
        writeUTF(log.flow.name)
        writeNullableEnum(log.mood)
        writeInt(log.symptoms.size)
        log.symptoms.sortedBy(Symptom::name).forEach { writeUTF(it.name) }
        writeUTF(log.note)
        writeNullableDouble(log.weightKg)
        writeNullableDouble(log.temperatureC)
        writeNullableDouble(log.sleepHours)
        writeNullableEnum(log.intimacy)
        writeNullableEnum(log.cervicalMucus)
        writeNullableEnum(log.ovulationTest)
        writeNullableEnum(log.pregnancyTest)
        writeNullableInt(log.painLevel)
        writeNullableEnum(log.energy)
        writeNullableEnum(log.stress)
        writeNullableEnum(log.activity)
        writeNullableEnum(log.medication)
        writeUTF(log.importedDetails)
    }

    private fun DataInputStream.readLog(): DayLog {
        val day = LocalDate.ofEpochDay(readLong())
        val bleeding = readBoolean()
        val spotting = readBoolean()
        val flow = readEnum(Flow.entries.toTypedArray())
        val mood = readNullableEnum(Mood.entries.toTypedArray())
        val symptomCount = readCount(Symptom.entries.size)
        val symptoms = buildSet { repeat(symptomCount) { add(readEnum(Symptom.entries.toTypedArray())) } }
        return DayLog(
            day = day,
            bleeding = bleeding,
            spotting = spotting,
            flow = flow,
            mood = mood,
            symptoms = symptoms,
            note = readUTF(),
            weightKg = readNullableDouble(),
            temperatureC = readNullableDouble(),
            sleepHours = readNullableDouble(),
            intimacy = readNullableEnum(Intimacy.entries.toTypedArray()),
            cervicalMucus = readNullableEnum(CervicalMucus.entries.toTypedArray()),
            ovulationTest = readNullableEnum(TestResult.entries.toTypedArray()),
            pregnancyTest = readNullableEnum(TestResult.entries.toTypedArray()),
            painLevel = readNullableInt(),
            energy = readNullableEnum(WellbeingLevel.entries.toTypedArray()),
            stress = readNullableEnum(WellbeingLevel.entries.toTypedArray()),
            activity = readNullableEnum(ActivityLevel.entries.toTypedArray()),
            medication = readNullableEnum(MedicationStatus.entries.toTypedArray()),
            importedDetails = readUTF(),
        )
    }

    private fun DataOutputStream.writeSnapshot(snapshot: ForecastSnapshot) {
        writeInt(snapshot.month.year)
        writeInt(snapshot.month.monthValue)
        writeLong(snapshot.periodStart.toEpochDay())
        writeLong(snapshot.earliestStart.toEpochDay())
        writeLong(snapshot.latestStart.toEpochDay())
        writeInt(snapshot.periodLength)
        writeBoolean(snapshot.reconstructed)
    }

    private fun DataInputStream.readSnapshot(): ForecastSnapshot = ForecastSnapshot(
        month = YearMonth.of(readInt(), readInt()),
        periodStart = LocalDate.ofEpochDay(readLong()),
        earliestStart = LocalDate.ofEpochDay(readLong()),
        latestStart = LocalDate.ofEpochDay(readLong()),
        periodLength = readInt(),
        reconstructed = readBoolean(),
    )

    private fun DataOutputStream.writeNullableInt(value: Int?) {
        writeBoolean(value != null)
        if (value != null) writeInt(value)
    }

    private fun DataInputStream.readNullableInt(): Int? = if (readBoolean()) readInt() else null

    private fun DataOutputStream.writeNullableDouble(value: Double?) {
        writeBoolean(value != null)
        if (value != null) writeDouble(value)
    }

    private fun DataInputStream.readNullableDouble(): Double? = if (readBoolean()) readDouble() else null

    private fun DataOutputStream.writeNullableDate(value: LocalDate?) {
        writeBoolean(value != null)
        if (value != null) writeLong(value.toEpochDay())
    }

    private fun DataInputStream.readNullableDate(): LocalDate? = if (readBoolean()) LocalDate.ofEpochDay(readLong()) else null

    private fun DataOutputStream.writeNullableEnum(value: Enum<*>?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value.name)
    }

    private fun <T : Enum<T>> DataInputStream.readNullableEnum(values: Array<T>): T? =
        if (readBoolean()) readEnum(values) else null

    private fun <T : Enum<T>> DataInputStream.readEnum(values: Array<T>): T {
        val name = readUTF()
        return values.firstOrNull { it.name == name }
            ?: throw MyCalendarFormatException("Unknown Selia backup value", failure = MyCalendarFailure.UNSUPPORTED)
    }

    private fun DataInputStream.readCount(max: Int): Int = readInt().also { if (it !in 0..max) throw IOException("Invalid count") }
}
