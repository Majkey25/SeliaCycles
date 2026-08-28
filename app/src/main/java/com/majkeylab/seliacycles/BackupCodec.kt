package com.majkeylab.seliacycles

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.DateTimeException
import java.time.LocalDate
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

object BackupCodec {
    const val MAX_FILE_BYTES = 16 * 1024 * 1024
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private val magic = "CYKLUS1".encodeToByteArray()

    fun encrypt(backup: CycleBackup, password: CharArray): ByteArray {
        require(password.size >= 8) { "Password must contain at least 8 characters" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(magic)
        val ciphertext = cipher.doFinal(toJson(backup).encodeToByteArray())
        return (magic + salt + iv + ciphertext).also {
            require(it.size <= MAX_FILE_BYTES) { "Backup is too large" }
        }
    }

    fun decrypt(data: ByteArray, password: CharArray): CycleBackup {
        if (data.size !in (magic.size + SALT_BYTES + IV_BYTES + 16)..MAX_FILE_BYTES) {
            throw BackupFormatException("Invalid backup size")
        }
        if (!data.copyOfRange(0, magic.size).contentEquals(magic)) {
            throw BackupFormatException("Unsupported backup file")
        }
        return try {
            val saltStart = magic.size
            val ivStart = saltStart + SALT_BYTES
            val contentStart = ivStart + IV_BYTES
            val salt = data.copyOfRange(saltStart, ivStart)
            val iv = data.copyOfRange(ivStart, contentStart)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
            cipher.updateAAD(magic)
            fromJson(cipher.doFinal(data.copyOfRange(contentStart, data.size)).decodeToString())
        } catch (error: GeneralSecurityException) {
            throw BackupFormatException("Wrong password or damaged backup", error)
        } catch (error: JSONException) {
            throw BackupFormatException("Invalid backup data", error)
        } catch (error: IllegalArgumentException) {
            throw BackupFormatException("Invalid backup data", error)
        } catch (error: DateTimeException) {
            throw BackupFormatException("Invalid backup date", error)
        }
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try {
                SecretKeySpec(bytes, "AES")
            } finally {
                bytes.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    private fun toJson(backup: CycleBackup): String = JSONObject()
        .put("version", 2)
        .put("settings", JSONObject()
            .put("cycleLength", backup.settings.cycleLength)
            .put("periodLength", backup.settings.periodLength)
            .put("firstDayOfWeek", backup.settings.firstDayOfWeek.name)
            .put("predictionsEnabled", backup.settings.predictionsEnabled)
            .put("reminderEnabled", backup.settings.reminderEnabled)
            .put("reminderDays", backup.settings.reminderDays)
            .put("theme", backup.settings.theme.name))
        .put("logs", JSONArray().also { logs ->
            backup.logs.sortedBy(DayLog::day).forEach { log ->
                logs.put(JSONObject()
                    .put("day", log.day.toString())
                    .put("bleeding", log.bleeding)
                    .put("flow", log.flow.name)
                    .put("mood", log.mood?.name ?: JSONObject.NULL)
                    .put("symptoms", JSONArray(log.symptoms.map(Symptom::name).sorted()))
                    .put("note", log.note)
                    .put("weightKg", log.weightKg ?: JSONObject.NULL)
                    .put("temperatureC", log.temperatureC ?: JSONObject.NULL)
                    .put("sleepHours", log.sleepHours ?: JSONObject.NULL)
                    .put("intimacy", log.intimacy?.name ?: JSONObject.NULL)
                    .put("importedDetails", log.importedDetails))
            }
        })
        .toString()

    internal fun fromJson(text: String): CycleBackup {
        val root = JSONObject(text)
        val version = root.getInt("version")
        if (version !in 1..2) throw BackupFormatException("Unsupported backup version")
        val settingsJson = root.getJSONObject("settings")
        val logsJson = root.getJSONArray("logs")
        if (logsJson.length() > CycleBackup.MAX_LOGS) throw BackupFormatException("Backup contains too many records")
        val logs = buildList(logsJson.length()) {
            repeat(logsJson.length()) { index ->
                val item = logsJson.getJSONObject(index)
                val symptomsJson = item.getJSONArray("symptoms")
                if (symptomsJson.length() > Symptom.entries.size) {
                    throw BackupFormatException("Invalid symptom list")
                }
                add(DayLog(
                    day = LocalDate.parse(item.getString("day")),
                    bleeding = item.getBoolean("bleeding"),
                    flow = Flow.valueOf(item.getString("flow")),
                    mood = item.optNullableString("mood")?.let(Mood::valueOf),
                    symptoms = buildSet {
                        repeat(symptomsJson.length()) { symptomIndex ->
                            add(Symptom.valueOf(symptomsJson.getString(symptomIndex)))
                        }
                    },
                    note = item.getString("note"),
                    weightKg = item.optNullableDouble("weightKg"),
                    temperatureC = item.optNullableDouble("temperatureC"),
                    sleepHours = item.optNullableDouble("sleepHours"),
                    intimacy = item.optNullableString("intimacy")?.let(Intimacy::valueOf),
                    importedDetails = item.optString("importedDetails"),
                ))
            }
        }
        return CycleBackup(
            logs = logs,
            settings = AppSettings(
                cycleLength = settingsJson.getInt("cycleLength"),
                periodLength = settingsJson.getInt("periodLength"),
                firstDayOfWeek = DayOfWeek.valueOf(settingsJson.getString("firstDayOfWeek")),
                predictionsEnabled = settingsJson.getBoolean("predictionsEnabled"),
                reminderEnabled = settingsJson.getBoolean("reminderEnabled"),
                reminderDays = settingsJson.getInt("reminderDays"),
                theme = AppTheme.valueOf(settingsJson.getString("theme")),
            ),
        )
    }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (!has(name) || isNull(name)) null else getDouble(name)

    private fun JSONObject.optNullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else getString(name).takeIf(String::isNotEmpty)
}
