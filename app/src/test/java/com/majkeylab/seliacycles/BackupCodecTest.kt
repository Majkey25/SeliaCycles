package com.majkeylab.seliacycles

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackupCodecTest {
    @Test
    fun encryptedBackupRoundTripsWithoutPlaintext() {
        val backup = CycleBackup(
            logs = listOf(
                DayLog(
                    day = LocalDate.of(2026, 8, 28),
                    bleeding = true,
                    flow = Flow.MEDIUM,
                    mood = Mood.OKAY,
                    symptoms = setOf(Symptom.CRAMPS, Symptom.FATIGUE),
                    note = "Private note",
                    weightKg = 68.4,
                    temperatureC = 36.6,
                    sleepHours = 7.5,
                    intimacy = Intimacy.PROTECTED,
                    importedDetails = "My Calendar mood code: 65",
                ),
                DayLog(
                    day = LocalDate.of(2026, 8, 29),
                    symptoms = setOf(Symptom.FATIGUE),
                ),
            ),
            settings = AppSettings(firstDayOfWeek = DayOfWeek.MONDAY),
        )

        val encrypted = BackupCodec.encrypt(backup, "correct horse".toCharArray())

        assertEquals(backup, BackupCodec.decrypt(encrypted, "correct horse".toCharArray()))
        assert(!encrypted.decodeToString().contains("Private note"))
    }

    @Test
    fun wrongPasswordFailsWithoutPartialData() {
        val encrypted = BackupCodec.encrypt(CycleBackup(), "correct horse".toCharArray())

        assertFailsWith<BackupFormatException> {
            BackupCodec.decrypt(encrypted, "wrong password".toCharArray())
        }
    }

    @Test
    fun readsVersionOneJson() {
        val backup = BackupCodec.fromJson(
            """{"version":1,"settings":{"cycleLength":28,"periodLength":5,"firstDayOfWeek":"MONDAY","predictionsEnabled":true,"reminderEnabled":false,"reminderDays":2,"theme":"SYSTEM"},"logs":[{"day":"2026-08-28","bleeding":true,"flow":"MEDIUM","mood":"GOOD","symptoms":["CRAMPS"],"note":"kept"}]}""",
        )

        assertEquals(DayLog(
            day = LocalDate.of(2026, 8, 28),
            bleeding = true,
            flow = Flow.MEDIUM,
            mood = Mood.GOOD,
            symptoms = setOf(Symptom.CRAMPS),
            note = "kept",
        ), backup.logs.single())
    }
}
